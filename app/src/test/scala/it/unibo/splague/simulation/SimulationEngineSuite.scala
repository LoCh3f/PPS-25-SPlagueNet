package it.unibo.splague.simulation

import it.unibo.splague.model.Probability
import it.unibo.splague.model.malware.{
  Malware,
  MalwareKind,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SimulationEngineSuite extends AnyFunSuite:

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

  private val nodeA = Node(
    NodeId.of("node-A").toOption.get,
    NodeType.Router,
    patchLevel = 0.2,
    defenseLevel = 0.5,
    state = NodeState.Healthy,
    workload = 0.3
  )

  private val nodeB = Node(
    NodeId.of("node-B").toOption.get,
    NodeType.Workstation,
    patchLevel = 0.4,
    defenseLevel = 0.7,
    state = NodeState.Infected,
    workload = 0.8
  )

  private val topology = Topology(
    nodes = Map(nodeA.nodeId.value -> nodeA, nodeB.nodeId.value -> nodeB),
    edges = Set.empty
  )

  private val scenario = Scenario(
    name = "Baseline",
    topology = topology,
    virus = malware,
    startingNode = nodeA,
    tick = 0,
    seed = 7,
    maxIterations = 3
  ).toOption.get

  test("run should keep applying the selected event until the max iteration is reached"):
    val selector = new SimulationUtils.EventSelector:
      override def nextEvent(scenario: Scenario): SimulationUtils.Event =
        new SimulationUtils.Event:
          override def apply(current: Scenario): Scenario =
            current.copy(seed = current.seed + 1)

    val result = new SimulationEngine(selector).run(scenario).toList
    result.map(_.tick) shouldBe List(0, 1, 2, 3)
    result.map(_.seed) shouldBe List(7, 8, 9, 10)

  test("run should stop before moving past maxIterations"):
    val selector = new SimulationUtils.EventSelector:
      override def nextEvent(scenario: Scenario): SimulationUtils.Event =
        new SimulationUtils.Event:
          override def apply(current: Scenario): Scenario =
            current.copy(name = s"${current.name}-step")

    val result = new SimulationEngine(selector).run(scenario).toList

    result.size shouldBe 4
    result.last.tick shouldBe 3
    result.last.name shouldBe "Baseline-step-step-step"
