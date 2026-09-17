package it.unibo.splague.update

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import it.unibo.splague.model.Awareness
import it.unibo.splague.model.node.NodeId.NodeId
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType}
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

  test("NodeType should be correctly encoded to a clean string"):
    val nodeType: NodeType = NodeType.Server
    val encoder = summon[Encoder[NodeType, FileFormat.Json]]

    encoder.encode(nodeType) shouldBe "\"Server\""

  test("NodeType should be correctly decoded from a clean string and keep its methods"):
    val json = "\"Server\""
    val decoder = summon[Decoder[NodeType, FileFormat.Json]]

    val result = decoder.decode(json)

    result shouldBe Right(NodeType.Server)
    result.toOption.get.detectionCoefficient shouldBe NodeType.Server.detectionCoefficient
    result.toOption.get.structuralVulnerability shouldBe NodeType.Server.structuralVulnerability

  test(
    "A JSON decoder should return a Persistence.Parsing error when an unknown NodeType is encountered"
  ):
    val json = "\"Unknown\""

    val decoder = summon[Decoder[NodeType, FileFormat.Json]]

    val state = decoder.decode(json)

    state.isLeft shouldBe true
    state.left.toOption.get match
      case PersistenceError.Parsing(_) => succeed
      case other                       => fail(s"Expected Parsing error, got $other")

  test("Awareness should be correctly encoded to a clean string"):
    val awareness: Awareness = Awareness.clamped(1.0)
    val encoder = summon[Encoder[Awareness, FileFormat.Json]]

    encoder.encode(awareness) shouldBe "1.0"

  test("Awareness should be correctly decoded from a clean string"):
    val json = "1.0"
    val decoder = summon[Decoder[Awareness, FileFormat.Json]]

    val result = decoder.decode(json)

    result shouldBe Right(Awareness.clamped(1.0))
    result.toOption.get.value shouldBe 1.0

  test(
    "A JSON decoder should return a Persistence.Parsing error when anything but a Double gets encountered"
  ):
    val json = "\"Unknown\""

    val decoder = summon[Decoder[Awareness, FileFormat.Json]]

    val state = decoder.decode(json)

    state.isLeft shouldBe true
    state.left.toOption.get match
      case PersistenceError.Parsing(_) => succeed
      case other                       => fail(s"Expected Parsing error, got $other")

  test("NodeId should be correctly encoded to a clean JSON string"):
    val nodeId = NodeId.of("node-1").toOption.get
    val encoder = summon[Encoder[NodeId, FileFormat.Json]]

    encoder.encode(nodeId) shouldBe "\"node-1\""

  test("NodeId should be correctly decoded from a clean JSON string"):
    val json = "\"node-1\""
    val decoder = summon[Decoder[NodeId, FileFormat.Json]]

    val result = decoder.decode(json)

    result shouldBe Right(NodeId.of("node-1").toOption.get)
    result.toOption.get.value shouldBe "node-1"

  test(
    "A JSON decoder should return a Persistence.Parsing error when an invalid NodeId (e.g., containing whitespace) is encountered"
  ):
    val json = "\"node 1\""
    val decoder = summon[Decoder[NodeId, FileFormat.Json]]

    val state = decoder.decode(json)

    state.isLeft shouldBe true
    println(state.left.toOption.get)
    state.left.toOption.get match
      case PersistenceError.Parsing(_) => succeed
      case other                       => fail(s"Expected Parsing error, got $other")

  test("Map[Node, Double] should be correctly encoded to JSON list of pairs and decoded back"):
    val nodeId = NodeId.of("node-1").toOption.get
    val node = Node.test(
      nodeId = nodeId,
      nodeType = NodeType.Server,
      state = NodeState.Healthy
    )

    val sampleMap: Map[Node, Double] = Map(node -> 0.75)

    val encoder = summon[Encoder[Map[Node, Double], FileFormat.Json]]
    val decoder = summon[Decoder[Map[Node, Double], FileFormat.Json]]

    // Encoding test
    val jsonString = encoder.encode(sampleMap)
    jsonString should not be empty

    // Decoding test
    val result = decoder.decode(jsonString)
    println(result)

    result.isRight shouldBe true
    val decodedMap = result.toOption.get
    decodedMap should have size 1
    decodedMap.head._2 shouldBe 0.75
    decodedMap.head._1.nodeId.value shouldBe "node-1"
