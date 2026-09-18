package it.unibo.splague.model.report

import it.unibo.splague.model.{Awareness, Probability, Scenario}
import it.unibo.splague.model.countermeasures.CountermeasureConfig
import it.unibo.splague.model.malware.{
  Malware,
  MalwareKind,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import org.junit.runner.RunWith
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class TickSummarySuite extends AnyFunSuite with Matchers with EitherValues:

  private val nodeId1 = NodeId.of("node-01").getOrElse(fail("Failed to create NodeId"))
  private val nodeId2 = NodeId.of("node-02").getOrElse(fail("Failed to create NodeId"))
  private val nodeId3 = NodeId.of("node-03").getOrElse(fail("Failed to create NodeId"))
  private val nodeId4 = NodeId.of("node-04").getOrElse(fail("Failed to create NodeId"))
  private val nodeId5 = NodeId.of("node-05").getOrElse(fail("Failed to create NodeId"))
  private val nodeId6 = NodeId.of("node-06").getOrElse(fail("Failed to create NodeId"))

  private val healthyNode1 =
    Node(nodeId1, NodeType.Workstation, 0.0, 0.0, NodeState.Healthy, 0.0, Set())
  private val healthyNode2 =
    Node(nodeId2, NodeType.Workstation, 0.0, 0.0, NodeState.Healthy, 0.0, Set())
  private val infectedNode =
    Node(nodeId3, NodeType.Workstation, 0.0, 0.0, NodeState.Infected, 0.0, Set())
  private val quarantinedNode =
    Node(nodeId4, NodeType.Workstation, 0.0, 0.0, NodeState.Quarantined, 0.0, Set())
  private val immuneNode =
    Node(nodeId5, NodeType.Workstation, 0.0, 0.0, NodeState.Immune, 0.0, Set())
  private val destroyedNode =
    Node(nodeId6, NodeType.Workstation, 0.0, 0.0, NodeState.Destroyed, 0.0, Set())

  private val topology = Topology(
    nodes = Map(
      "node-01" -> healthyNode1,
      "node-02" -> healthyNode2,
      "node-03" -> infectedNode,
      "node-04" -> quarantinedNode,
      "node-05" -> immuneNode,
      "node-06" -> destroyedNode
    ),
    edges = Set.empty
  )

  private val malware = Malware(
    name = "TestWorm",
    kind = MalwareKind.Worm,
    traits = MalwareTraits(
      infectivity = Probability.clamped(0.5),
      stealth = Probability.clamped(0.5),
      payloadSeverity = PayloadSeverityLevel.Low,
      persistence = Probability.clamped(0.5),
      footprint = Probability.clamped(0.5)
    ),
    vectors = Set(PropagationVector.NetworkExploit)
  ).getOrElse(fail("Failed to create Malware"))

  private val awareness = Awareness(0.42).getOrElse(fail("Failed to create Awareness"))

  private val scenario = Scenario(
    name = "test-scenario",
    topology = topology,
    virus = malware,
    startingNode = healthyNode1,
    tick = 3,
    seed = 7,
    maxIterations = 10,
    countermeasureConfig = CountermeasureConfig.empty,
    awareness = awareness
  ).getOrElse(fail("Failed to create Scenario"))

  test("TickSummary.from captures tick, node-state counts, and awareness"):
    val summary = TickSummary.from(scenario)

    summary.tick shouldBe 3
    summary.healthy shouldBe 2
    summary.infected shouldBe 1
    summary.quarantined shouldBe 1
    summary.immune shouldBe 1
    summary.destroyed shouldBe 1
    summary.awareness shouldBe 0.42 +- 0.0001
