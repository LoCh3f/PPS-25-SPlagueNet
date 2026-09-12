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
