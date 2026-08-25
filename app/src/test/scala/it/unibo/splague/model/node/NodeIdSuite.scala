package it.unibo.splague.model.node

import org.junit.runner.RunWith
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class NodeIdSuite extends AnyFunSuite with Matchers with EitherValues:
  test("NodeId.of should succeed with a well formatted node id"):
    val result = NodeId.of("node-01")
    result.isRight shouldBe true
    result.map(_.value) shouldBe Right("node-01")

  test("NodeId.of should fail if the id is empty or blank"):
    NodeId.of("") shouldBe Left("The ID cannot be empty")
    NodeId.of("  ") shouldBe Left("The ID cannot be empty")

  test("NodeId.of should fail if it contains whitespace"):
    NodeId.of("node 01") shouldBe Left("The ID cannot contain white space")
