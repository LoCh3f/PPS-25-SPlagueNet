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
import it.unibo.splague.persistence.FileFormat
import it.unibo.splague.update.messages.{
  NavigationUpdate,
  NavigationUpdateSuite,
  ScenarioFormUpdate,
  ScenarioFormUpdateSuite,
  ScenarioIOUpdate,
  ScenarioIOUpdateSuite,
  ScenarioLifecycleUpdate,
  ScenarioLifecycleUpdateSuite,
  SimulationUpdate,
  SimulationUpdateSuite
}
import it.unibo.splague.view.form.ScenarioForm
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import java.nio.file.Path

/** `Mvu.update` itself holds no behavior beyond dispatch: each category's actual behavior is
  * covered by that handler's own suite ([[NavigationUpdateSuite]], [[ScenarioFormUpdateSuite]],
  * [[ScenarioLifecycleUpdateSuite]], [[SimulationUpdateSuite]], [[ScenarioIOUpdateSuite]]). This
  * suite only checks the wiring: that every `Msg` case reaches the handler responsible for it.
  */
@RunWith(classOf[JUnitRunner])
final class MvuSuite extends AnyFunSuite with Matchers:

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

  private def freshState: AppState = AppState.init(ModelState())

  test("navigation messages are dispatched to NavigationUpdate"):
    Mvu.update(Msg.GoToMenu, freshState) shouldBe
      NavigationUpdate.update(Msg.GoToMenu, freshState)

  test("scenario-form messages are dispatched to ScenarioFormUpdate"):
    val editing = freshState.copy(scenarioForm = Some(ScenarioForm.fromScenario(baseline)))
    val msg = Msg.UpdateMalware(editing.scenarioForm.get.virus)

    Mvu.update(msg, editing) shouldBe ScenarioFormUpdate.update(msg, editing)

  test("scenario-lifecycle messages are dispatched to ScenarioLifecycleUpdate"):
    val state = freshState.copy(model = ModelState(scenarios = Vector(baseline)))
    val msg = Msg.SelectScenario(baseline.name)

    Mvu.update(msg, state) shouldBe ScenarioLifecycleUpdate.update(msg, state)

  test("simulation messages are dispatched to SimulationUpdate"):
    Mvu.update(Msg.ResetSimulation, freshState) shouldBe
      SimulationUpdate.update(Msg.ResetSimulation, freshState)

  test("scenario import/export messages are dispatched to ScenarioIOUpdate"):
    val msg = Msg.ImportScenario(FileFormat.Json, Path.of("path/does-not-exist.json"))

    Mvu.update(msg, freshState) shouldBe ScenarioIOUpdate.update(msg, freshState)
