package it.unibo.splague.update

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import it.unibo.splague.model.Probability
import it.unibo.splague.model.malware.{
  Malware,
  MalwareKind,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}

@RunWith(classOf[JUnitRunner])
final class AwarenessRulesSuite extends AnyFunSuite with Matchers:

  private def malwareWithStealth(stealth: Double): Malware =
    val traits = MalwareTraits(
      infectivity = Probability(0.5).toOption.get,
      stealth = Probability(stealth).toOption.get,
      payloadSeverity = PayloadSeverityLevel.Low,
      persistence = Probability(0.5).toOption.get,
      footprint = Probability(0.3).toOption.get
    )
    Malware(
      name = "TestMalware",
      kind = MalwareKind.Worm,
      traits = traits,
      vectors = Set(PropagationVector.NetworkExploit)
    ).toOption.get

  private def nodeWith(
      id: String,
      workload: Double,
      nodeType: NodeType = NodeType.Workstation
  ): Node =
    val nodeId = NodeId.of(id).getOrElse(fail("Failed to create NodeId"))
    Node(nodeId, nodeType, patchLevel = 0.0, defenseLevel = 0.0, NodeState.Healthy, workload, Set())

  private def topologyOf(nodes: Node*): Topology =
    Topology(nodes = nodes.map(n => n.nodeId.value -> n).toMap, edges = Set.empty)

  test("detectionSignal is 0.0 for an empty topology"):
    val malware = malwareWithStealth(0.0)
    val topology = Topology(nodes = Map.empty, edges = Set.empty)

    AwarenessRules.detectionSignal(topology, malware) shouldBe 0.0

  test("detectionSignal for a single node combines workload, detectionCoefficient and stealth"):
    val stealth = 0.4
    val workload = 0.5
    val malware = malwareWithStealth(stealth)
    val node = nodeWith("n1", workload = workload, nodeType = NodeType.Server)
    val topology = topologyOf(node)

    val expected = workload * NodeType.Server.detectionCoefficient * (1 - stealth)
    AwarenessRules.detectionSignal(topology, malware) shouldBe expected +- 0.0001

  test("detectionSignal averages contributions across multiple nodes"):
    val malware = malwareWithStealth(0.0)
    val n1 = nodeWith("n1", workload = 1.0, nodeType = NodeType.Workstation)
    val n2 = nodeWith("n2", workload = 0.0, nodeType = NodeType.Router)

    val topology = topologyOf(n1, n2)

    val signal1 = 1.0 * NodeType.Workstation.detectionCoefficient
    val signal2 = 0.0 * NodeType.Router.detectionCoefficient
    val expected = (signal1 + signal2) / 2

    AwarenessRules.detectionSignal(topology, malware) shouldBe expected +- 0.0001

  test("detectionSignal is 0.0 when malware stealth is 1.0, regardless of workload"):
    val malware = malwareWithStealth(1.0)
    val node = nodeWith("n1", workload = 1.0, nodeType = NodeType.IoTDevice)
    val topology = topologyOf(node)

    AwarenessRules.detectionSignal(topology, malware) shouldBe 0.0 +- 0.0001

  test("detectionSignal increases as node workload increases, for fixed stealth"):
    val malware = malwareWithStealth(0.2)
    val lowLoad = topologyOf(nodeWith("n1", workload = 0.1))
    val highLoad = topologyOf(nodeWith("n1", workload = 0.9))

    AwarenessRules.detectionSignal(highLoad, malware) should be > AwarenessRules.detectionSignal(
      lowLoad,
      malware
    )

  test("detectionSignal is unaffected by node type when only one node type is present twice"):
    val malware = malwareWithStealth(0.0)
    val a = nodeWith("a", workload = 0.5, nodeType = NodeType.Router)
    val b = nodeWith("b", workload = 0.5, nodeType = NodeType.Router)

    val single = topologyOf(a)
    val doubled = topologyOf(a, b)

    // averaging keeps the per-node signal the same when all nodes are identical
    AwarenessRules.detectionSignal(doubled, malware) shouldBe AwarenessRules.detectionSignal(
      single,
      malware
    ) +- 0.0001
