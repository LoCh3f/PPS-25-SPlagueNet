package it.unibo.splague.persistence

import it.unibo.splague.model.{Probability, Scenario}
import it.unibo.splague.model.malware.{
  Malware,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.malware.MalwareKind.Worm
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import java.nio.file.{Files, Path}

@RunWith(classOf[JUnitRunner])
final class ScenarioPersistenceSuite extends AnyFunSuite with Matchers:

  import it.unibo.splague.persistence.codecs.json.CodecCatalog.given
  import it.unibo.splague.persistence.codecs.json.JsonCodec.given

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

  val defaultScenario = Scenario(
    name = "Full Simulation Test",
    topology = topology,
    virus = dummyVirus,
    startingNode = nodeValid,
    tick = 5,
    seed = 123,
    maxIterations = 50
  )

  private val repo = Repository.json[Scenario]

  test("saving and loading a Scenario as JSON round-trips to an equal value"):
    val tempFile = Files.createTempFile("scenario-test", ".json")
    val scenario = defaultScenario.toOption.get

    try
      val saved = repo.save(scenario, tempFile)
      assert(saved.isRight)

      val loaded = repo.load(tempFile)
      loaded shouldBe Right(scenario)
    finally Files.deleteIfExists(tempFile)

  test("loading from a non-existent path returns a PersistenceError.IO"):
    val result = repo.load(Path.of("path/fake.json"))

    result match
      case Left(PersistenceError.IO(_)) => succeed
      case other                        => fail(s"expected PersistenceError.IO, got $other")

  test("loading malformed JSON returns a PersistenceError.Parsing"):
    val tempFile = Files.createTempFile("malformed-scenario", ".json")
    Files.writeString(tempFile, "{ not a valid json ")

    try
      val result = repo.load(tempFile)
      result match
        case Left(PersistenceError.Parsing(_)) => succeed
        case other => fail(s"expected PersistenceError.Parsing, obtained $other")
    finally Files.deleteIfExists(tempFile)
