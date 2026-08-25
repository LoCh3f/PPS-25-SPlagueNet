package it.unibo.splague.model.node

import it.unibo.splague.model.Probability
import it.unibo.splague.model.connection.Connection
import it.unibo.splague.model.connection.Connection.Edge
import it.unibo.splague.model.node.NodeState.{Healthy, Immune}
import it.unibo.splague.model.node.NodeType.Router
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NodeTest extends AnyFunSuite:
  private val packetLoss = Probability.apply(0.1).getOrElse(fail("packet loss should be valid"))

  private val nodeId1 = NodeId.of("node-01").getOrElse(fail("Failed to create NodeId"))
  private val nodeId2 = NodeId.of("node-02").getOrElse(fail("Failed to create NodeId"))
  private val nodeIde3 = NodeId.of("node-03").getOrElse(fail("Failed to create NodeId"))

  private val node1 = Node(nodeId1, NodeType.Router, 0.1, 0.2, NodeState.Infected, 0.5)
  private val node2 = Node(nodeId2, NodeType.MobileDevice, 0.0, 0.1, NodeState.Healthy, 0.1)
  private val node3 = Node(nodeIde3, NodeType.Server, 0.4, 0.5, NodeState.Infected, 0.3)

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
    val nodeId = NodeId.of("node-01").getOrElse(fail("Failed to create NodeId"))
    val node = Node(
      nodeId,
      NodeType.Router,
      0.2,
      0.5,
      Healthy,
      0.5
    )

    node.nodeId shouldBe nodeId
    node.nodeType shouldBe NodeType.Router
    node.patchLevel shouldBe 0.2
    node.defenseLevel shouldBe 0.5
    node.state shouldBe NodeState.Healthy
    node.workload shouldBe 0.5

  test("NodeId.of should succeed with a well formatted node id"):
    val result = NodeId.of("node-01")
    result.isRight shouldBe true
    result.map(_.value) shouldBe Right("node-01")

  test("NodeId.of should fail if the id is empty or blank"):
    NodeId.of("") shouldBe Left("The ID cannot be empty")
    NodeId.of("  ") shouldBe Left("The ID cannot be empty")

  test("NodeId.of should fail if it contains whitespace"):
    NodeId.of("node 01") shouldBe Left("The ID cannot contain white space")

  test("NodeState enum should contain all expected states"):
    val states = NodeState.values
    states should contain allOf (
      NodeState.Healthy,
      NodeState.Infected,
      NodeState.Quarantined,
      NodeState.Immune,
      NodeState.Destroyed
    )
