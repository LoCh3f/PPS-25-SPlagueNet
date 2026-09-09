package it.unibo.splague.dsl

import it.unibo.splague.model.node.NodeType.Workstation
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
