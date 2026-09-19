package it.unibo.splague.persistence.codecs.txt

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
import it.unibo.splague.persistence.codecs.txt.TextCodec.given
import it.unibo.splague.persistence.codecs.txt.CodecCatalog.given

@RunWith(classOf[JUnitRunner])
final class CodecSuite extends AnyFunSuite with Matchers:

  test(
    "Scenario should be successfully encoded to TXT via persistence framework"
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

    val encoder = summon[Encoder[Scenario, FileFormat.Txt.type]]

    val txtString = encoder.encode(originalScenario)
    txtString should not be empty

    txtString should include("=== PLAGUENET SCENARIO EXPORT ===")
    txtString should include(s"Name: ${originalScenario.name}")
    txtString should include(s"Tick: ${originalScenario.tick}")
    txtString should include(s"Seed: ${originalScenario.seed}")
    txtString should include(s"Max Iterations: ${originalScenario.maxIterations}")
    txtString should include(s"node-01")
