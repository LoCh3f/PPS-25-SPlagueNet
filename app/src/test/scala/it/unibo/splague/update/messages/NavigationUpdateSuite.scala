package it.unibo.splague.update.messages

import it.unibo.splague.AppState
import it.unibo.splague.model.malware.*
import it.unibo.splague.model.node.*
import it.unibo.splague.model.{ModelState, Probability, Scenario}
import it.unibo.splague.update.Msg
import it.unibo.splague.update.messages.NavigationUpdate
import it.unibo.splague.update.simulation.SimulationState
import it.unibo.splague.update.simulation.event.SimulationEvents.{Event, EventSelector}
import it.unibo.splague.update.simulation.report.ScenarioReport
import it.unibo.splague.view.Screen
import it.unibo.splague.view.form.ScenarioForm
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class NavigationUpdateSuite extends AnyFunSuite with Matchers:

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

  test("GoToMenu switches to the Menu screen"):
    val onSimulation = AppState.init(ModelState()).copy(screen = Screen.Simulation)

    val result = NavigationUpdate.update(Msg.GoToMenu, onSimulation)

    result.screen shouldBe Screen.Menu

  test("GoToSimulation only switches screens when a scenarioForm is already open"):
    val form = ScenarioForm.fromScenario(baseline)
    val editing = AppState
      .init(ModelState(currentScenario = Some(baseline)))
      .copy(screen = Screen.Menu, scenarioForm = Some(form))

    val result = NavigationUpdate.update(Msg.GoToSimulation, editing)

    result.screen shouldBe Screen.Simulation
    result.scenarioForm shouldBe Some(form)

  test("GoToSimulation builds a form from model.currentScenario when none is open"):
    val browsing = AppState.init(ModelState(currentScenario = Some(baseline)))

    val result = NavigationUpdate.update(Msg.GoToSimulation, browsing)

    result.screen shouldBe Screen.Simulation
    result.scenarioForm.map(_.name) shouldBe Some(baseline.name)

  test(
    "GoToSimulation falls back to the built-in linear scenario when there is no current scenario"
  ):
    val fresh = AppState.init(ModelState())

    val result = NavigationUpdate.update(Msg.GoToSimulation, fresh)

    result.screen shouldBe Screen.Simulation
    result.scenarioForm shouldBe defined

  test(
    "GoToReport switches to the Report screen and computes a report once the simulation has finished"
  ):
    val finished = stateWithSimulation(running = false)

    val result = NavigationUpdate.update(Msg.GoToReport, finished)

    result.screen shouldBe Screen.Report
    result.report shouldBe Some(ScenarioReport.from(baseline, noOpSelector))
    result.errors shouldBe Vector.empty

  test(
    "GoToReport switches to the Report screen without computing a report while the simulation is still running"
  ):
    val running = stateWithSimulation(running = true)

    val result = NavigationUpdate.update(Msg.GoToReport, running)

    result.screen shouldBe Screen.Report
    result.report shouldBe running.report
    result.errors shouldBe Vector.empty

  test(
    "GoToReport switches to the Report screen without computing a report when there is no simulation"
  ):
    val noSimulation = AppState.init(ModelState())

    val result = NavigationUpdate.update(Msg.GoToReport, noSimulation)

    result.screen shouldBe Screen.Report
    result.report shouldBe None
    result.errors shouldBe Vector.empty

  test("ImportReport stores the given report and clears errors"):
    val report = ScenarioReport.from(baseline, noOpSelector)
    val state = AppState.init(ModelState())

    val result = NavigationUpdate.update(Msg.ImportReport(report), state)

    result.report shouldBe Some(report)
    result.errors shouldBe Vector.empty
