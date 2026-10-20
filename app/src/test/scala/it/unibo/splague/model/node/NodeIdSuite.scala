package it.unibo.splague.model.node

import it.unibo.splague.utils.config.UIConfig.ErrorMessages
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
    NodeId.of("") shouldBe Left(ErrorMessages.EMPTY_NODE_ID)
    NodeId.of("  ") shouldBe Left(ErrorMessages.EMPTY_NODE_ID)

  test("NodeId.of should fail if it contains whitespace"):
    NodeId.of("node 01") shouldBe Left(ErrorMessages.NODE_ID_WHITESPACE)
