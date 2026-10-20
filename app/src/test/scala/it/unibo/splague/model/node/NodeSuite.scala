package it.unibo.splague.model.node

import it.unibo.splague.model.Probability
import it.unibo.splague.model.connection.Connection
import it.unibo.splague.model.connection.Connection.Edge
import it.unibo.splague.model.malware.PropagationVector.{NetworkExploit, Phishing}
import it.unibo.splague.model.node.NodeState.{Healthy, Immune}
import it.unibo.splague.model.node.NodeType.Router
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import it.unibo.splague.utils.config.SimulationConfig.{Detection, Vulnerability}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NodeSuite extends AnyFunSuite:
  private val packetLoss = Probability.apply(0.1).getOrElse(fail("packet loss should be valid"))

  private val nodeId1 = NodeId
    .of("node-01")
    .fold(
      err => throw new IllegalStateException(s"Failed to create test NodeId: $err"),
      id => id
    )
  private val nodeId2 = NodeId
    .of("node-02")
    .fold(
      err => throw new IllegalStateException(s"Failed to create test NodeId: $err"),
      id => id
    )
  private val nodeId3 = NodeId
    .of("node-03")
    .fold(
      err => throw new IllegalStateException(s"Failed to create test NodeId: $err"),
      id => id
    )

  private val node1 =
    Node(nodeId1, NodeType.Router, 0.1, 0.2, NodeState.Infected, 0.5, Set(Phishing)).getOrElse(
      fail()
    )
  private val node2 = Node(
    nodeId2,
    NodeType.MobileDevice,
    0.0,
    0.1,
    NodeState.Healthy,
    0.1,
    Set(Phishing, NetworkExploit)
  ).getOrElse(fail())
  private val node3 =
    Node(nodeId3, NodeType.Server, 0.4, 0.5, NodeState.Infected, 0.3, Set(NetworkExploit))
      .getOrElse(fail())

  test("NodeType values and coefficients should match expected values"):
    NodeType.IoTDevice.detectionCoefficient shouldBe Detection.IOT_DEVICE_COEFFICIENT
    NodeType.IoTDevice.structuralVulnerability shouldBe Vulnerability.IOT_DEVICE_FACTOR

    NodeType.Workstation.detectionCoefficient shouldBe Detection.WORKSTATION_COEFFICIENT
    NodeType.Workstation.structuralVulnerability shouldBe Vulnerability.WORKSTATION_FACTOR

    NodeType.Router.detectionCoefficient shouldBe Detection.ROUTER_COEFFICIENT
    NodeType.Router.structuralVulnerability shouldBe Vulnerability.ROUTER_FACTOR

    NodeType.Server.detectionCoefficient shouldBe Detection.SERVER_COEFFICIENT
    NodeType.Server.structuralVulnerability shouldBe Vulnerability.SERVER_FACTOR

    NodeType.MobileDevice.detectionCoefficient shouldBe Detection.MOBILE_DEVICE_COEFFICIENT
    NodeType.MobileDevice.structuralVulnerability shouldBe Vulnerability.MOBILE_DEVICE_FACTOR

  test("Node instance should be created with correct properties"):
    val nodeId = NodeId.of("node-01").getOrElse(fail("Failed to create NodeId"))
    val node = Node(
      nodeId,
      NodeType.Router,
      0.2,
      0.5,
      Healthy,
      0.5,
      Set()
    ).getOrElse(fail())

    node.nodeId shouldBe nodeId
    node.nodeType shouldBe NodeType.Router
    node.patchLevel shouldBe 0.2
    node.defenseLevel shouldBe 0.5
    node.state shouldBe NodeState.Healthy
    node.workload shouldBe 0.5

  test("NodeState enum should contain all expected states"):
    val states = NodeState.values
    states should contain allOf (
      NodeState.Healthy,
      NodeState.Infected,
      NodeState.Quarantined,
      NodeState.Immune,
      NodeState.Destroyed
    )

  test("Node.test() should create a Node with default fields for testing"):
    val node = Node.test(nodeId = nodeId1).getOrElse(fail("Failed to create Node"))

    node.workload shouldBe Node.defaultWorkload
    node.state shouldBe Node.defaultState
    node.defenseLevel shouldBe Node.defaultDefenseLevel
    node.patchLevel shouldBe Node.defaultPatchLevel
    node.nodeType shouldBe Node.defaultNodeType
