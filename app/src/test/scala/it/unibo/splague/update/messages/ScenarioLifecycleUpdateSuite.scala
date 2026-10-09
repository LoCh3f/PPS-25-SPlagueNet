package it.unibo.splague.update.messages

import it.unibo.splague.AppState
import it.unibo.splague.model.malware.*
import it.unibo.splague.model.node.*
import it.unibo.splague.model.{ModelState, Probability, Scenario}
import it.unibo.splague.update.Msg
import it.unibo.splague.update.messages.ScenarioLifecycleUpdate
import it.unibo.splague.update.simulation.SimulationState
import it.unibo.splague.update.simulation.event.SimulationEvents.{Event, EventSelector}
import it.unibo.splague.update.simulation.report.ScenarioReport
import it.unibo.splague.view.form.ScenarioForm
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class ScenarioLifecycleUpdateSuite extends AnyFunSuite with Matchers:

  private val validTraits = (for
    infectivity <- Probability(0.6)
    stealth <- Probability(0.4)
    persistence <- Probability(0.5)
    footprint <- Probability(0.3)
  yield MalwareTraits(
    infectivity,
    stealth,
    payloadSeverity = PayloadSeverityLevel.Low,
    persistence,
    footprint
  )).toOption.get

  private val malware = Malware(
    name = "TestMalware",
    kind = MalwareKind.Worm,
    traits = validTraits,
    vectors = Set(PropagationVector.NetworkExploit)
  ).toOption.get

  private val node = Node(
    NodeId.of("node-A").toOption.get,
    NodeType.Router,
    patchLevel = 0.2,
    defenseLevel = 0.5,
    state = NodeState.Healthy,
    workload = 0.3,
    Set()
  )

  private val topology = Topology(
    nodes = Map(node.nodeId.value -> node),
    edges = Set.empty
  )

  private val baseline = Scenario(
    name = "Baseline",
    topology = topology,
    virus = malware,
    startingNode = node,
    tick = 0,
    seed = 7,
    maxIterations = 3
  ).toOption.get

  private val noOpEvent: Event = (s: Scenario) => s
  private val noOpSelector: EventSelector = (_: Scenario) => noOpEvent

  private def stateWithSimulation(running: Boolean): AppState =
    AppState
      .init(ModelState())
      .copy(
        simulation = Some(
          SimulationState(
            initial = baseline,
            selector = noOpSelector,
            states = LazyList(baseline),
            current = baseline,
            running = running
          )
        )
      )

  test("SaveScenario adds a brand-new scenario, and its malware, to the model state"):
    val editing = AppState
      .init(ModelState())
      .copy(scenarioForm = Some(ScenarioForm.fromScenario(baseline)))

    val result = ScenarioLifecycleUpdate.update(Msg.SaveScenario, editing)

    result.model.scenarios.map(_.name) shouldBe Vector(baseline.name)
    result.model.malwares.map(_.name) shouldBe Vector(malware.name)
    result.model.currentScenario.map(_.name) shouldBe Some(baseline.name)
    result.errors shouldBe Vector.empty

  test("SaveScenario updates the existing entry sharing its name instead of duplicating it"):
    val editedForm = ScenarioForm.fromScenario(baseline).copy(seed = "99")

    val editing = AppState
      .init(ModelState(scenarios = Vector(baseline), malwares = Vector(malware)))
      .copy(scenarioForm = Some(editedForm))

    val result = ScenarioLifecycleUpdate.update(Msg.SaveScenario, editing)

    result.model.scenarios.map(_.name) shouldBe Vector(baseline.name)
    result.model.scenarios.map(_.seed) shouldBe Vector(99)
    result.model.malwares.map(_.name) shouldBe Vector(malware.name)

  test(
    "SaveScenario adds a new entry under the new name when the scenario is renamed, keeping the old one"
  ):
    val renamedForm = ScenarioForm.fromScenario(baseline).copy(name = "Renamed Baseline")

    val editing = AppState
      .init(
        ModelState(
          scenarios = Vector(baseline),
          malwares = Vector(malware),
          currentScenario = Some(baseline)
        )
      )
      .copy(scenarioForm = Some(renamedForm))

    val result = ScenarioLifecycleUpdate.update(Msg.SaveScenario, editing)

    result.model.scenarios.map(_.name) shouldBe Vector(baseline.name, "Renamed Baseline")
    result.model.currentScenario.map(_.name) shouldBe Some("Renamed Baseline")

  test(
    "SaveScenario adds a new malware entry under the new name when the virus is renamed, keeping the old one"
  ):
    val baseForm = ScenarioForm.fromScenario(baseline)
    val renamedVirusForm = baseForm.copy(virus = baseForm.virus.copy(name = "Renamed Malware"))

    val editing = AppState
      .init(
        ModelState(
          scenarios = Vector(baseline),
          malwares = Vector(malware),
          currentScenario = Some(baseline)
        )
      )
      .copy(scenarioForm = Some(renamedVirusForm))

    val result = ScenarioLifecycleUpdate.update(Msg.SaveScenario, editing)

    result.model.malwares.map(_.name) shouldBe Vector(malware.name, "Renamed Malware")

  test("SaveScenario reports an error when no scenario form is open"):
    val state = AppState.init(ModelState())

    val result = ScenarioLifecycleUpdate.update(Msg.SaveScenario, state)

    result.errors should not be Vector.empty

  test("SelectScenario switches to the picked scenario, clearing any prior simulation and report"):
    val advanced = baseline.copy(tick = 1)

    val browsing = AppState
      .init(ModelState(scenarios = Vector(baseline)))
      .copy(
        simulation = Some(
          SimulationState(
            initial = baseline,
            selector = noOpSelector,
            states = LazyList.empty,
            current = advanced,
            running = false
          )
        ),
        report = Some(ScenarioReport.from(baseline, noOpSelector))
      )

    val result = ScenarioLifecycleUpdate.update(Msg.SelectScenario(baseline.name), browsing)

    result.model.currentScenario shouldBe Some(baseline)
    result.scenarioForm.map(_.name) shouldBe Some(baseline.name)
    // Both belonged to the run of a scenario that is no longer open: keeping them around would
    // let Report or Reset silently act on the wrong scenario.
    result.simulation shouldBe None
    result.report shouldBe None
    result.errors shouldBe Vector.empty

  test("SelectScenario is refused while the simulation is still running"):
    val running = stateWithSimulation(running = true)
      .copy(model = ModelState(scenarios = Vector(baseline)))

    val result = ScenarioLifecycleUpdate.update(Msg.SelectScenario(baseline.name), running)

    result.simulation shouldBe running.simulation
    result.errors should not be Vector.empty

  test("SelectScenario reports an error for an unknown scenario name"):
    val state = AppState.init(ModelState(scenarios = Vector(baseline)))

    val result = ScenarioLifecycleUpdate.update(Msg.SelectScenario("missing"), state)

    result.errors should not be Vector.empty

  test("CancelScenario restores the form from model.currentScenario, discarding unsaved edits"):
    val editedForm = ScenarioForm.fromScenario(baseline).copy(name = "Unsaved edit")
    val editing = AppState
      .init(ModelState(currentScenario = Some(baseline)))
      .copy(scenarioForm = Some(editedForm))

    val result = ScenarioLifecycleUpdate.update(Msg.CancelScenario, editing)

    result.scenarioForm.map(_.name) shouldBe Some(baseline.name)
    result.errors shouldBe Vector.empty

  test("CancelScenario just clears errors when there is no saved scenario to restore"):
    val state = AppState
      .init(ModelState())
      .copy(errors = Vector(it.unibo.splague.view.ValidationError("x", "boom")))

    val result = ScenarioLifecycleUpdate.update(Msg.CancelScenario, state)

    result.scenarioForm shouldBe None
    result.errors shouldBe Vector.empty
