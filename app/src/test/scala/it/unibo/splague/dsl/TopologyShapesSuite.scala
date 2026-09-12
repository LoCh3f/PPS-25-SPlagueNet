package it.unibo.splague.dsl

import it.unibo.splague.model.connection.Connection.ChannelType.LAN
import it.unibo.splague.model.node.NodeType.{Router, Workstation}
import org.junit.runner.RunWith
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class TopologyShapesSuite extends AnyFunSuite with Matchers with EitherValues:

  test("star topology declares one hub and the requested number of leaves"):
    val result = topology:
      star("hub", Router, "leaf", 3, Workstation, LAN)

    val topo = result.value
    topo.nodes.keys should contain allOf ("hub", "leaf0", "leaf1", "leaf2")
    topo.nodes.size shouldBe 4

  test("star topology connects every leaf to the hub, and only to the hub"):
    val result = topology:
      star("hub", Router, "leaf", 3, Workstation, LAN)

    val topo = result.value
    topo.edges.size shouldBe 3
    topo.edges.foreach { edge =>
      Set(edge.source.nodeId.value, edge.target.nodeId.value) should contain("hub")
    }

  test("star topology with zero leaves declares only the hub, no edges"):
    val result = topology:
      star("hub", Router, "leaf", 0, Workstation, LAN)

    val topo = result.value
    topo.nodes.keys should contain only "hub"
    topo.edges shouldBe empty

  test("ring topology forms a cycle where every node has exactly two neighbors"):
    val result = topology:
      ring("n", 4, Workstation, LAN)

    val topo = result.value
    topo.nodes.size shouldBe 4
    topo.edges.size shouldBe 4
    topo.nodes.values.foreach(n => topo.degree(n) shouldBe 2)

  test("ring topology with zero nodes declares nothing"):
    val result = topology:
      ring("n", 0, Workstation, LAN)

    val topo = result.value
    topo.nodes shouldBe empty
    topo.edges shouldBe empty

  test("ring topology with one node declares only that node, no self-loop edge"):
    val result = topology:
      ring("n", 1, Workstation, LAN)

    val topo = result.value
    topo.nodes.keys should contain only "n0"
    topo.edges shouldBe empty

  test("ring topology with two nodes declares exactly one edge, not a duplicate"):
    val result = topology:
      ring("n", 2, Workstation, LAN)

    val topo = result.value
    topo.nodes.keys should contain allOf ("n0", "n1")
    topo.edges.size shouldBe 1

  test("mesh topology connects every pair of nodes exactly once"):
    val result = topology:
      mesh("n", 4, Workstation, LAN)

    val topo = result.value
    topo.nodes.size shouldBe 4
    topo.edges.size shouldBe 6 // C(4,2)

  test("mesh topology with zero or one node declares no edges"):
    val zeroResult = topology:
      mesh("n", 0, Workstation, LAN)
    zeroResult.value.edges shouldBe empty

    val oneResult = topology:
      mesh("n", 1, Workstation, LAN)
    oneResult.value.nodes.keys should contain only "n0"
    oneResult.value.edges shouldBe empty
