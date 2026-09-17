package it.unibo.splague.update

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import io.circe.syntax.*
import io.circe.parser.*
import io.circe.Codec
import it.unibo.splague.model.node.NodeState
import it.unibo.splague.persistence.{Encoder, FileFormat}
import org.scalatest.matchers.should.Matchers
import it.unibo.splague.persistence.JsonCodecs.given

@RunWith(classOf[JUnitRunner])
final class CodecSuite extends AnyFunSuite with Matchers:
  import it.unibo.splague.persistence.JsonCodecs

  test("A JSON encoder should correctly encode a NodeState into a JSON object"):
    val nodeState = NodeState.Healthy

    val encoder = summon[Encoder[NodeState, FileFormat.Json]]

    val rawJson = encoder.encode(nodeState)
    rawJson shouldBe "\"Healthy\""
