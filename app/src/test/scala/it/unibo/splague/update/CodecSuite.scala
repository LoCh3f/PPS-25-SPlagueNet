package it.unibo.splague.update

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import io.circe.syntax.*
import io.circe.parser.*
import io.circe.Codec
import it.unibo.splague.model.node.NodeState
import it.unibo.splague.persistence.{Decoder, Encoder, FileFormat, PersistenceError}
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

  test("A JSON decoder should correctly decode a JSON NodeState object into a NodeState"):
    val json = "\"Healthy\""

    val decoder = summon[Decoder[NodeState, FileFormat.Json]]

    val state = decoder.decode(json)
    state shouldBe Right(NodeState.Healthy)

  test(
    "A JSON decoder should return a Persistence.Parsing error when an unknown NodeState is encountered"
  ):
    val json = "\"Unknown\""

    val decoder = summon[Decoder[NodeState, FileFormat.Json]]

    val state = decoder.decode(json)

    state.isLeft shouldBe true
    state.left.toOption.get match
      case PersistenceError.Parsing(_) => succeed
      case other                       => fail(s"Expected Parsing error, got $other")
