package it.unibo.splague.dsl

import it.unibo.splague.model.connection.Connection.ChannelType.{LAN, WAN}
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

    val errors = result.left.value
    errors should contain("The ID cannot contain white space")
    errors should contain("The ID cannot be empty")

  test("a connected pair of nodes builds a topology with one edge"):
    val result = topology:
      node("A", Workstation)
      node("B", Server)
      "A" <-> "B" via LAN

    val topo = result.value
    topo.nodes.keys should contain allOf ("A", "B")
    topo.edges.size shouldBe 1

  test("an edge referencing an unknown node id should be reported as an error"):
    val result = topology:
      node("A", Workstation)
      "A" <-> "ghost" via LAN

    result.left.value should contain("Edge references unknown node id: ghost")

  test("an edge with both sides unknown reports both missing ids"):
    val result = topology:
      "ghost1" <-> "ghost2" via LAN

    val errors = result.left.value
    errors should contain("Edge references unknown node id: ghost1")
    errors should contain("Edge references unknown node id: ghost2")

  test("a self-loop edge should be reported as an error"):
    val result = topology:
      node("A", Workstation)
      "A" <-> "A" via LAN

    result.left.value should contain("Self-loop edges are not allowed: A")

  test("a self-loop referencing an unknown node reports only the self-loop error"):
    val result = topology:
      "ghost" <-> "ghost" via LAN

    val errors = result.left.value
    errors shouldBe List("Self-loop edges are not allowed: ghost")

  test("a duplicate undirected edge should be reported as an error"):
    val result = topology:
      node("A", Workstation)
      node("B", Server)
      "A" <-> "B" via LAN
      "A" <-> "B" via LAN

    result.left.value should contain("Duplicate edge between A and B")

  test("a duplicate edge declared in reverse order should also be reported"):
    val result = topology:
      node("A", Workstation)
      node("B", Server)
      "A" <-> "B" via LAN
      "B" <-> "A" via LAN

    result.left.value should contain("Duplicate edge between A and B")

  test("edge references should be normalized the same way node ids are"):
    val result = topology:
      node("A", Workstation)
      node("B", Server)
      " A" <-> "B " via LAN

    val topo = result.value
    topo.edges.size shouldBe 1

  test("duplicate edge detection should normalize ids, catching whitespace-padded duplicates"):
    val result = topology:
      node("A", Workstation)
      node("B", Server)
      "A" <-> "B " via LAN
      "A" <-> "B" via LAN

    result.left.value should contain("Duplicate edge between A and B")

  test("an edge referencing a malformed, undeclared id reports it as an unknown node id"):
    val result = topology:
      node("A", Workstation)
      "bad id" <-> "A" via LAN

    result.left.value shouldBe List("Edge references unknown node id: bad id")

  test("a self-loop with a malformed id reports the self-loop, not a format error"):
    val result = topology:
      "bad id" <-> "bad id" via LAN

    result.left.value shouldBe List("Self-loop edges are not allowed: bad id")

  test(
    "a malformed id reused as an edge endpoint produces its node error plus its own edge errors"
  ):
    val result = topology:
      node("bad id", Workstation)
      "bad id" <-> "ghost" via LAN

    val errors = result.left.value
    errors should contain("The ID cannot contain white space")
    errors should contain("Edge references unknown node id: bad id")
    errors should contain("Edge references unknown node id: ghost")

  test("via should apply override parameters over channel defaults"):
    val result = topology:
      node("A", Workstation)
      node("B", Server)
      "A" <-> "B" via (WAN, bandwidth = Option(50.0))

    val topo = result.value
    val edge = topo.edges.head
    edge.channel.channelType shouldBe WAN
    edge.channel.bandwidth shouldBe 50.0

  test("errors from nodes and edges accumulate together in the same result"):
    val result = topology:
      node("bad id", Workstation)
      node("A", Workstation)
      "bad id" <-> "ghost" via LAN

    val errors = result.left.value
    errors should contain("The ID cannot contain white space")
    errors should contain("Edge references unknown node id: ghost")
