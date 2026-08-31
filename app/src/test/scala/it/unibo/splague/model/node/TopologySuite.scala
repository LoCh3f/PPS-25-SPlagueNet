package it.unibo.splague.model.node

import it.unibo.splague.model.Probability
import it.unibo.splague.model.connection.Connection
import it.unibo.splague.model.connection.Connection.Edge
import org.junit.runner.RunWith
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
final class TopologySuite extends AnyFunSuite with Matchers with EitherValues:
  private val packetLoss = Probability.apply(0.1).getOrElse(fail("packet loss should be valid"))

  private val channel = Connection.Channel(
    channelType = Connection.ChannelType.VPN,
    bandwidth = 250.0,
    latency = 15.0,
    jitter = 1.0,
    packetLoss = packetLoss
  )

  private val nodeId1 = NodeId.of("node-01").getOrElse(fail("Failed to create NodeId"))
  private val nodeId2 = NodeId.of("node-02").getOrElse(fail("Failed to create NodeId"))
  private val nodeIde3 = NodeId.of("node-03").getOrElse(fail("Failed to create NodeId"))

  private val node1 = Node(nodeId1, NodeType.Router, 0.1, 0.2, NodeState.Infected, 0.5, Set())
  private val node2 = Node(nodeId2, NodeType.MobileDevice, 0.0, 0.1, NodeState.Healthy, 0.1, Set())
  private val node3 = Node(nodeIde3, NodeType.Server, 0.4, 0.5, NodeState.Infected, 0.3, Set())

  test("Topology should correctly store nodes and edges"):
    val nodesMap = Map("node-01" -> node1, "node-02" -> node2, "node-03" -> node3)

    val edgeN1N2 = Connection.Edge(node1, node2, channel, None)
    val edgeN1N3 = Connection.Edge(node1, node3, channel, None)

    val topology = Topology(nodes = nodesMap, edges = Set(edgeN1N2, edgeN1N3))

    topology.nodes.size shouldBe 3
    topology.nodes.contains("node-01") shouldBe true
    topology.nodes("node-01").state shouldBe NodeState.Infected
    topology.edges should contain allOf (
      edgeN1N2,
      edgeN1N3
    )

  test("Topology should correctly store nodes and handle empty edges"):
    val nodesMap = Map("node-01" -> node1, "node-02" -> node2, "node-03" -> node3)
    val topology = Topology(nodes = nodesMap, edges = Set.empty)

    topology.nodes.size shouldBe 3
    topology.nodes.contains("node-01") shouldBe true
    topology.nodes("node-01").state shouldBe NodeState.Infected
    topology.edges shouldBe Matchers.empty

  /** Topology.neighbors tests
    */
  test("Topology.neighbors should return empty set for source node with 0 neighbors"):
    val nodesMap = Map("node-01" -> node1)
    val topology = Topology(nodes = nodesMap, edges = Set.empty)

    val neighborsOfN1 = topology.neighbors(node1)
    neighborsOfN1 shouldBe Matchers.empty

  test(
    "Topology.neighbors should return the node itself if thee source node is connected to itself"
  ):
    val edgeN1N1 = Connection.Edge(node1, node1, channel, None)

    val nodesMap = Map("node-01" -> node1)
    val topology = Topology(nodes = nodesMap, edges = Set(edgeN1N1))

    val neighborsOfN1 = topology.neighbors(node1)
    neighborsOfN1 should contain only (node1)

  test("Topology.neighbors should return all the neighbouring nodes of a source node with 2 edges"):
    val edgeN1N2 = Connection.Edge(node1, node2, channel, None)
    val edgeN1N3 = Connection.Edge(node1, node3, channel, None)

    val nodesMap = Map("node-01" -> node1, "node-02" -> node2, "node-03" -> node3)
    val topology = Topology(nodes = nodesMap, edges = Set(edgeN1N2, edgeN1N3))

    val neighborsOfN1 = topology.neighbors(node1)
    neighborsOfN1 should contain only (node2, node3)

  /** Topology.edgesof tests
    */
  test("Topology.edgesOf on a single node with no edges should return an empty set"):
    val nodesMap = Map("node-01" -> node1)
    val topology = Topology(nodes = nodesMap, edges = Set.empty)

    topology.edgesOf(node1) shouldBe Matchers.empty

  test("Topology.edgesOf on a node with one edge should return a set containing only that edge"):
    val nodesMap = Map("node-01" -> node1, "node-02" -> node2)
    val edgeN1N2 = Connection.Edge(node1, node2, channel, None)

    val topology = Topology(nodes = nodesMap, edges = Set(edgeN1N2))

    topology.edgesOf(node1) should contain only edgeN1N2

  test(
    "Topology.edgesOf on a node with multiple edges should return a set containing only those edges"
  ):
    val nodesMap = Map("node-01" -> node1, "node-02" -> node2, "node-03" -> node3)
    val edgeN1N1 = Connection.Edge(node1, node1, channel, None)
    val edgeN1N2 = Connection.Edge(node1, node2, channel, None)
    val edgeN1N3 = Connection.Edge(node1, node3, channel, None)

    val topology = Topology(nodes = nodesMap, edges = Set(edgeN1N2, edgeN1N3, edgeN1N1))

    topology.edgesOf(node1) should contain only (edgeN1N2, edgeN1N3, edgeN1N1)

  /** Topology.degree tests
    */
  test("degree should return 0 for an isolated node"):
    val topology = Topology(
      nodes = Map(nodeId1.value -> node1),
      edges = Set.empty
    )
    topology.degree(node1) shouldBe 0

  test("degree should return the correct count of connected neighbors"):
    val edge1 = Edge(node1, node2, channel, None)
    val edge2 = Edge(node1, node3, channel, None)

    val topology = Topology(
      nodes = Map(nodeId1.value -> node1, nodeId2.value -> node2, nodeIde3.value -> node3),
      edges = Set(edge1, edge2)
    )

    topology.degree(node1) shouldBe 2
    topology.degree(node2) shouldBe 1

  /** Topology.hub tests
    */
  test("hub should return the node with the highest degree"):
    val edge1 = Edge(node1, node2, channel, None)
    val edge2 = Edge(node1, node3, channel, None)

    val topology = Topology(
      nodes = Map(nodeId1.value -> node1, nodeId2.value -> node2, nodeIde3.value -> node3),
      edges = Set(edge1, edge2)
    )

    topology.hub(node1) shouldBe Some(node1)

  test("hub should return None if topology is empty"):
    val emptyTopology = Topology(nodes = Map.empty, edges = Set.empty)
    emptyTopology.hub(node1) shouldBe None

  /** Topology.reachableFrom tests
    */
  test("Topology reachableFrom should find all connected nodes via BFS"):
    val edge1 = Edge(node1, node2, channel, None)
    val edge2 = Edge(node1, node3, channel, None)

    val topology = Topology(
      nodes = Map(nodeId1.value -> node1, nodeId2.value -> node2, nodeIde3.value -> node3),
      edges = Set(edge1, edge2)
    )

    val reachable = topology.reachableFrom(node1)
    reachable should contain allOf (node2, node3)
    reachable.size shouldBe 2
