package it.unibo.splague.persistence.codecs.json

import it.unibo.splague.model.connection.Connection.{Channel, ChannelType, Edge}
import it.unibo.splague.model.countermeasures.Countermeasures
import it.unibo.splague.model.malware.MalwareKind.Worm
import it.unibo.splague.model.malware.{
  Malware,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.NodeId.NodeId
import it.unibo.splague.model.node.*
import it.unibo.splague.model.{Awareness, Probability, Scenario}
import it.unibo.splague.persistence.{FileFormat, PersistenceError}
import it.unibo.splague.persistence.codecs.json.JsonCodec.given
import it.unibo.splague.persistence.codecs.{Decoder, Encoder}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class CodecSuite extends AnyFunSuite with Matchers:

  import it.unibo.splague.persistence.codecs.json.JsonCodec.given
  import it.unibo.splague.persistence.codecs.json.CodecCatalog.given

  test("A JSON encoder should correctly encode a NodeState into a JSON object"):
    val nodeState = NodeState.Healthy

    val encoder = summon[Encoder[NodeState, FileFormat.Json.type]]

    val rawJson = encoder.encode(nodeState)
    rawJson shouldBe "\"Healthy\""

  test("A JSON decoder should correctly decode a JSON NodeState object into a NodeState"):
    val json = "\"Healthy\""

    val decoder = summon[Decoder[NodeState, FileFormat.Json.type]]

    val state = decoder.decode(json)
    state shouldBe Right(NodeState.Healthy)

  test(
    "A JSON decoder should return a Persistence.Parsing error when an unknown NodeState is encountered"
  ):
    val json = "\"Unknown\""

    val decoder = summon[Decoder[NodeState, FileFormat.Json.type]]

    val state = decoder.decode(json)

    state.isLeft shouldBe true
    state.left.toOption.get match
      case PersistenceError.Parsing(_) => succeed
      case other                       => fail(s"Expected Parsing error, got $other")

  test("NodeType should be correctly encoded to a clean string"):
    val nodeType: NodeType = NodeType.Server
    val encoder = summon[Encoder[NodeType, FileFormat.Json.type]]

    encoder.encode(nodeType) shouldBe "\"Server\""

  test("NodeType should be correctly decoded from a clean string and keep its methods"):
    val json = "\"Server\""
    val decoder = summon[Decoder[NodeType, FileFormat.Json.type]]

    val result = decoder.decode(json)

    result shouldBe Right(NodeType.Server)
    result.toOption.get.detectionCoefficient shouldBe NodeType.Server.detectionCoefficient
    result.toOption.get.structuralVulnerability shouldBe NodeType.Server.structuralVulnerability

  test(
    "A JSON decoder should return a Persistence.Parsing error when an unknown NodeType is encountered"
  ):
    val json = "\"Unknown\""

    val decoder = summon[Decoder[NodeType, FileFormat.Json.type]]

    val state = decoder.decode(json)

    state.isLeft shouldBe true
    state.left.toOption.get match
      case PersistenceError.Parsing(_) => succeed
      case other                       => fail(s"Expected Parsing error, got $other")

  test("Awareness should be correctly encoded to a clean string"):
    val awareness: Awareness = Awareness.clamped(1.0)
    val encoder = summon[Encoder[Awareness, FileFormat.Json.type]]

    encoder.encode(awareness) shouldBe "1.0"

  test("Awareness should be correctly decoded from a clean string"):
    val json = "1.0"
    val decoder = summon[Decoder[Awareness, FileFormat.Json.type]]

    val result = decoder.decode(json)

    result shouldBe Right(Awareness.clamped(1.0))
    result.toOption.get.value shouldBe 1.0

  test(
    "A JSON decoder should return a Persistence.Parsing error when anything but a Double gets encountered"
  ):
    val json = "\"Unknown\""

    val decoder = summon[Decoder[Awareness, FileFormat.Json.type]]

    val state = decoder.decode(json)

    state.isLeft shouldBe true
    state.left.toOption.get match
      case PersistenceError.Parsing(_) => succeed
      case other                       => fail(s"Expected Parsing error, got $other")

  test("NodeId should be correctly encoded to a clean JSON string"):
    val nodeId = NodeId.of("node-1").toOption.get
    val encoder = summon[Encoder[NodeId, FileFormat.Json.type]]

    encoder.encode(nodeId) shouldBe "\"node-1\""

  test("NodeId should be correctly decoded from a clean JSON string"):
    val json = "\"node-1\""
    val decoder = summon[Decoder[NodeId, FileFormat.Json.type]]

    val result = decoder.decode(json)

    result shouldBe Right(NodeId.of("node-1").toOption.get)
    result.toOption.get.value shouldBe "node-1"

  test(
    "A JSON decoder should return a Persistence.Parsing error when an invalid NodeId (e.g., containing whitespace) is encountered"
  ):
    val json = "\"node 1\""
    val decoder = summon[Decoder[NodeId, FileFormat.Json.type]]

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

    val encoder = summon[Encoder[Map[Node, Double], FileFormat.Json.type]]
    val decoder = summon[Decoder[Map[Node, Double], FileFormat.Json.type]]

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

  test("Topology with nodes and edges should be correctly encoded and decoded"):
    val id1 = NodeId.of("node-1").toOption.get
    val id2 = NodeId.of("node-2").toOption.get

    val node1 = Node.test(nodeId = id1, nodeType = NodeType.Server, state = NodeState.Healthy)
    val node2 = Node.test(nodeId = id2, nodeType = NodeType.Workstation, state = NodeState.Healthy)

    val channel = Channel.default(ChannelType.LAN)
    val edge = Edge(node1, node2, channel, None)

    val topology = Topology(
      nodes = Map(id1.value -> node1, id2.value -> node2),
      edges = Set(edge)
    )

    val encoder = summon[Encoder[Topology, FileFormat.Json.type]]
    val decoder = summon[Decoder[Topology, FileFormat.Json.type]]

    val json = encoder.encode(topology)
    val result = decoder.decode(json)

    result.isRight shouldBe true
    result.toOption.get.edges should have size 1

  test("Countermeasures enum should be correctly encoded and decoded"):
    val cm = Countermeasures.Firewall
    val encoder = summon[Encoder[Countermeasures, FileFormat.Json.type]]
    val decoder = summon[Decoder[Countermeasures, FileFormat.Json.type]]

    val json = encoder.encode(cm)
    json shouldBe "\"Firewall\""

    decoder.decode(json) shouldBe Right(Countermeasures.Firewall)

  test("Map[Double, Countermeasures] should be correctly encoded and decoded"):
    val map: Map[Double, Countermeasures] = Map(0.5 -> Countermeasures.Patch)
    val encoder = summon[Encoder[Map[Double, Countermeasures], FileFormat.Json.type]]
    val decoder = summon[Decoder[Map[Double, Countermeasures], FileFormat.Json.type]]

    val json = encoder.encode(map)
    val result = decoder.decode(json)

    result.isRight shouldBe true
    result.toOption.get.get(0.5) shouldBe Some(Countermeasures.Patch)

  test(
    "Scenario should be successfully encoded to JSON and decoded back via Persistence framework"
  ):
    val id1 = NodeId.of("node-01").getOrElse(fail())
    val id2 = NodeId.of("node-02").getOrElse(fail())

    val nodeValid = Node(id1, NodeType.Router, 0.1, 0.2, NodeState.Healthy, 0.0, Set())
    val nodeInvalid = Node(id2, NodeType.Server, 0.0, 0.1, NodeState.Healthy, 0.0, Set())

    val topology = Topology(
      nodes = Map("node-01" -> nodeValid),
      edges = Set.empty
    )

    val validTraits = (for
      infectivity <- Probability(0.6)
      stealth <- Probability(0.4)
      persistence <- Probability(0.5)
      footprint <- Probability(0.3)
    yield MalwareTraits(
      infectivity,
      stealth,
      payloadSeverity = PayloadSeverityLevel.Low,
      persistence,
      footprint
    )).toOption.get

    val dummyVirus = Malware(
      "dummy",
      Worm,
      validTraits,
      vectors = Set(PropagationVector.NetworkExploit)
    ).toOption.get

    val scenarioCreationResult = Scenario(
      name = "Full Simulation Test",
      topology = topology,
      virus = dummyVirus,
      startingNode = nodeValid,
      tick = 5,
      seed = 123,
      maxIterations = 50
    )

    scenarioCreationResult.isRight shouldBe true
    val originalScenario = scenarioCreationResult.toOption.get

    val encoder = summon[Encoder[Scenario, FileFormat.Json.type]]
    val decoder = summon[Decoder[Scenario, FileFormat.Json.type]]

    val jsonString = encoder.encode(originalScenario)
    jsonString should not be empty

    val decodedResult = decoder.decode(jsonString)
    decodedResult.isRight shouldBe true

    val decodedScenario = decodedResult.toOption.get

    decodedScenario.name shouldBe originalScenario.name
    decodedScenario.maxIterations shouldBe originalScenario.maxIterations
    decodedScenario.seed shouldBe originalScenario.seed
    decodedScenario.tick shouldBe originalScenario.tick
    decodedScenario.startingNode.nodeId shouldBe originalScenario.startingNode.nodeId
