package it.unibo.splague.model

import it.unibo.splague.model.NodeState.{Healthy, Immune}
import it.unibo.splague.model.NodeType.Router
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NodeTest extends AnyFunSuite:
  test("NodeType values and coefficients should match expected values"):
    NodeType.IoTDevice.detectionCoefficient shouldBe 0.3
    NodeType.IoTDevice.structuralVulnerability shouldBe 1.3

    NodeType.Workstation.detectionCoefficient shouldBe 1.5
    NodeType.Workstation.structuralVulnerability shouldBe 0.8

    NodeType.Router.detectionCoefficient shouldBe 0.3
    NodeType.Router.structuralVulnerability shouldBe 1.3

    NodeType.Server.detectionCoefficient shouldBe 0.3
    NodeType.Server.structuralVulnerability shouldBe 1.3

    NodeType.MobileDevice.detectionCoefficient shouldBe 0.3
    NodeType.MobileDevice.structuralVulnerability shouldBe 1.3

  test("Node instance should be created with correct properties"):
    val node = Node(
      "node-01",
      NodeType.Router,
      0.2,
      0.5,
      Healthy,
      0.5
    )

    node.nodeId shouldBe "node-01"
    node.nodeType shouldBe NodeType.Router
    node.patchLevel shouldBe 0.2
    node.defenseLevel shouldBe 0.5
    node.state shouldBe NodeState.Healthy
    node.workload shouldBe 0.5

  test("Topology should correctly store nodes and handle empty connections"):
    val node1 = Node("node-01", NodeType.Router, 0.1, 0.2, NodeState.Infected, 0.5)
    val node2 = Node("node-02", NodeType.MobileDevice, 0.0, 0.1, NodeState.Healthy, 0.1)

    val nodesMap = Map("node-01" -> node1, "node-02" -> node2)
    val topology = Topology(nodes = nodesMap, connections = Set.empty)

    topology.nodes.size shouldBe 2
    topology.nodes.contains("node-01") shouldBe true
    topology.nodes("node-01").state shouldBe NodeState.Infected
    topology.connections shouldBe Matchers.empty

  test("NodeState enum should contain all expected states"):
    val states = NodeState.values
    states should contain allOf (
      NodeState.Healthy,
      NodeState.Infected,
      NodeState.Quarantined,
      NodeState.Immune,
      NodeState.Destroyed
    )
