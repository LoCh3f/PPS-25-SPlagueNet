package it.unibo.splague.dsl

import it.unibo.splague.model.connection.Connection.ChannelType.LAN
import it.unibo.splague.model.node.NodeType.{Server, Workstation}
import org.junit.runner.RunWith
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class TopologyDslSuite extends AnyFunSuite with Matchers with EitherValues:

  test("a single node declaration builds a topology with one node and no edges"):
    val result = topology:
      node("A", Workstation)

    val topo = result.value
    topo.nodes.keys should contain only "A"
    topo.edges shouldBe empty

  test("a malformed node id should be reported as an error"):
    val result = topology:
      node("bad id", Workstation)

    result.left.value should contain("The ID cannot contain white space")

  test("duplicate node ids should be reported as an accumulated error"):
    val result = topology:
      node("A", Workstation)
      node("A", Workstation)

    result.left.value should contain("Duplicate node id: A")

  test("errors from multiple malformed nodes accumulate rather than short-circuit"):
    val result = topology:
      node("bad id", Workstation)
      node("", Server)

    result.left.value should contain allOf (
      "The ID cannot contain white space",
      "The ID cannot be empty"
    )

  test("a connected pair of nodes builds a topology with one edge"):
    val result = topology:
      node("A", Workstation)
      node("B", Server)
      "A" <-> "B" via LAN

    val topo = result.value
    topo.nodes.keys should contain allOf ("A", "B")
    topo.edges.size shouldBe 1
