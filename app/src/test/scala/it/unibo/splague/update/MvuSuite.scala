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

  test("ResetSimulation restores the initial scenario and clears the simulation once finished"):
    val advanced = baseline.copy(tick = 1)

    val finished = AppState
      .init(ModelState(currentScenario = Some(advanced)))
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
    result.model.currentScenario shouldBe Some(baseline)
    result.scenarioForm.map(_.tick) shouldBe Some(baseline.tick.toString)
    result.errors shouldBe Vector.empty

  test("ResetSimulation is refused while the simulation is still running"):
    val running = stateWithSimulation(running = true)

    val result = Mvu.update(Msg.ResetSimulation, running)

    result.simulation shouldBe running.simulation
    result.errors should not be Vector.empty

  test("ResetSimulation is refused when there is no simulation to reset"):
    val noSimulation = AppState.init(ModelState())

    val result = Mvu.update(Msg.ResetSimulation, noSimulation)

    result.errors should not be Vector.empty
