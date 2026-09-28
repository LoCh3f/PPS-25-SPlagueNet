package it.unibo.splague.update

import it.unibo.splague.AppState
import it.unibo.splague.model.{ModelState, Probability, Scenario}
import it.unibo.splague.model.malware.{
  Malware,
  MalwareKind,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import it.unibo.splague.update.simulation.SimulationState
import it.unibo.splague.update.simulation.event.SimulationEvents.{Event, EventSelector}
import it.unibo.splague.update.simulation.report.ScenarioReport
import it.unibo.splague.view.Screen
import it.unibo.splague.view.form.ScenarioForm
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.not
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MvuSuite extends AnyFunSuite:

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

  test("GoToReport switches to the Report screen once the simulation has finished"):
    val finished = stateWithSimulation(running = false)

    val result = Mvu.update(Msg.GoToReport, finished)

    result.screen shouldBe Screen.Report
    result.errors shouldBe Vector.empty

  test("GoToReport is refused while the simulation is still running"):
    val running = stateWithSimulation(running = true)

    val result = Mvu.update(Msg.GoToReport, running)

    result.screen shouldBe running.screen
    result.errors should not be Vector.empty

  test("GoToReport is refused when there is no simulation to report on"):
    val noSimulation = AppState.init(ModelState())

    val result = Mvu.update(Msg.GoToReport, noSimulation)

    result.errors should not be Vector.empty

  test("SimulationStep advances the simulation and scenarioForm without touching currentScenario"):
    val advanced = baseline.copy(tick = 1)

    val stepping = AppState
      .init(ModelState(currentScenario = Some(baseline)))
      .copy(
        scenarioForm = Some(ScenarioForm.fromScenario(baseline)),
        simulation = Some(
          SimulationState(
            initial = baseline,
            selector = noOpSelector,
            states = LazyList(advanced),
            current = baseline,
            running = true
          )
        )
      )

    val result = Mvu.update(Msg.SimulationStep, stepping)

    result.simulation.map(_.current) shouldBe Some(advanced)
    result.scenarioForm.map(_.tick) shouldBe Some(advanced.tick.toString)
    // model.currentScenario identifies the scenario this session is working on (set on
    // open/select/save/cancel/start/reset/import); it must not follow every simulated tick.
    result.model.currentScenario shouldBe Some(baseline)

  test("StartSimulation seeds simulation.initial but keeps model.currentScenario pre-seed"):
    val starting = AppState
      .init(ModelState())
      .copy(scenarioForm = Some(ScenarioForm.fromScenario(baseline)))

    val result = Mvu.update(Msg.StartSimulation, starting)

    result.simulation.map(_.initial.topology.nodes(node.nodeId.value).state) shouldBe
      Some(NodeState.Infected)
    // model.currentScenario is the scenario's identity, not a run snapshot: it must stay at the
    // pre-seed, all-Healthy scenario so ResetSimulation can restore a genuinely clean scenario.
    result.model.currentScenario.map(_.topology.nodes(node.nodeId.value).state) shouldBe
      Some(NodeState.Healthy)
    result.errors shouldBe Vector.empty

  test(
    "ResetSimulation restores model.currentScenario, which stays Healthy though simulation.initial was seeded"
  ):
    val seededNode = node.copy(state = NodeState.Infected)
    val seeded = baseline.copy(
      topology = topology.copy(nodes = topology.nodes.updated(node.nodeId.value, seededNode))
    )

    val finished = AppState
      .init(ModelState(currentScenario = Some(baseline))) // pre-seed: patient zero still Healthy
      .copy(
        scenarioForm = Some(ScenarioForm.fromScenario(seeded)),
        simulation = Some(
          SimulationState(
            initial = seeded, // deliberately different: ScenarioReport needs this one seeded
            selector = noOpSelector,
            states = LazyList.empty,
            current = seeded,
            running = false
          )
        )
      )

    val result = Mvu.update(Msg.ResetSimulation, finished)

    result.simulation shouldBe None
    result.model.currentScenario shouldBe Some(baseline)
    result.scenarioForm.get.topology.nodes.map(_.state) shouldBe Vector(NodeState.Healthy)
    result.errors shouldBe Vector.empty

  test(
    "ResetSimulation restores model.currentScenario even when it diverged from simulation.initial"
  ):
    // Simulates saving mid-run: model.currentScenario was overwritten by SaveScenario with
    // whatever the form held at that point (here: a different, already-infected scenario), so
    // Reset should now restore *that*, not simulation.initial.
    val advanced = baseline.copy(tick = 1)
    val savedMidRun = baseline.copy(name = "Baseline (saved mid-run)", tick = 1)

    val finished = AppState
      .init(ModelState(currentScenario = Some(savedMidRun)))
      .copy(
        scenarioForm = Some(ScenarioForm.fromScenario(advanced)),
        simulation = Some(
          SimulationState(
            initial = baseline,
            selector = noOpSelector,
            states = LazyList.empty,
            current = advanced,
            running = false
          )
        )
      )

    val result = Mvu.update(Msg.ResetSimulation, finished)

    result.simulation shouldBe None
    result.model.currentScenario shouldBe Some(savedMidRun)
    result.scenarioForm.map(_.name) shouldBe Some(savedMidRun.name)
    result.errors shouldBe Vector.empty

  test("ResetSimulation is refused when there is a finished simulation but no current scenario"):
    val finishedNoScenario = AppState
      .init(ModelState())
      .copy(
        simulation = Some(
          SimulationState(
            initial = baseline,
            selector = noOpSelector,
            states = LazyList.empty,
            current = baseline,
            running = false
          )
        )
      )

    val result = Mvu.update(Msg.ResetSimulation, finishedNoScenario)

    result.simulation shouldBe finishedNoScenario.simulation
    result.errors should not be Vector.empty

  test("ResetSimulation is refused while the simulation is still running"):
    val running = stateWithSimulation(running = true)

    val result = Mvu.update(Msg.ResetSimulation, running)

    result.simulation shouldBe running.simulation
    result.errors should not be Vector.empty

  test("ResetSimulation is refused when there is no simulation to reset"):
    val noSimulation = AppState.init(ModelState())

    val result = Mvu.update(Msg.ResetSimulation, noSimulation)

    result.errors should not be Vector.empty

  test("SaveScenario adds a brand-new scenario, and its malware, to the model state"):
    val editing = AppState
      .init(ModelState())
      .copy(scenarioForm = Some(ScenarioForm.fromScenario(baseline)))

    val result = Mvu.update(Msg.SaveScenario, editing)

    result.model.scenarios.map(_.name) shouldBe Vector(baseline.name)
    result.model.malwares.map(_.name) shouldBe Vector(malware.name)
    result.model.currentScenario.map(_.name) shouldBe Some(baseline.name)
    result.errors shouldBe Vector.empty

  test("SaveScenario updates the existing entry sharing its name instead of duplicating it"):
    val editedForm = ScenarioForm.fromScenario(baseline).copy(seed = "99")

    val editing = AppState
      .init(ModelState(scenarios = Vector(baseline), malwares = Vector(malware)))
      .copy(scenarioForm = Some(editedForm))

    val result = Mvu.update(Msg.SaveScenario, editing)

    result.model.scenarios.map(_.name) shouldBe Vector(baseline.name)
    result.model.scenarios.map(_.seed) shouldBe Vector(99)
    result.model.malwares.map(_.name) shouldBe Vector(malware.name)

  test("SaveScenario replaces the previously-named entry when the scenario is renamed"):
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

    val result = Mvu.update(Msg.SaveScenario, editing)

    result.model.scenarios.map(_.name) shouldBe Vector("Renamed Baseline")

  test("SaveScenario replaces the previously-named malware entry when the virus is renamed"):
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

    val result = Mvu.update(Msg.SaveScenario, editing)

    result.model.malwares.map(_.name) shouldBe Vector("Renamed Malware")

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

    val result = Mvu.update(Msg.SelectScenario(baseline.name), browsing)

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

    val result = Mvu.update(Msg.SelectScenario(baseline.name), running)

    result.simulation shouldBe running.simulation
    result.errors should not be Vector.empty

  test("SelectScenario reports an error for an unknown scenario name"):
    val state = AppState.init(ModelState(scenarios = Vector(baseline)))

    val result = Mvu.update(Msg.SelectScenario("missing"), state)

    result.errors should not be Vector.empty
