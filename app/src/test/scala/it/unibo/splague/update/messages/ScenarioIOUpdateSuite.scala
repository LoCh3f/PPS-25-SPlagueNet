package it.unibo.splague.update.messages

import it.unibo.splague.AppState
import it.unibo.splague.model.malware.*
import it.unibo.splague.model.node.*
import it.unibo.splague.model.{ModelState, Probability, Scenario}
import it.unibo.splague.persistence.codecs.json.CodecCatalog.given
import it.unibo.splague.persistence.codecs.json.JsonCodec.given
import it.unibo.splague.persistence.{ExportPaths, FileFormat, Repository}
import it.unibo.splague.update.Msg
import it.unibo.splague.update.messages.ScenarioIOUpdate
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import java.nio.file.{Files, Path}

@RunWith(classOf[JUnitRunner])
final class ScenarioIOUpdateSuite extends AnyFunSuite with Matchers:

  private val validTraits = (for
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

  private val malware = Malware(
    name = "TestMalware",
    kind = MalwareKind.Worm,
    traits = validTraits,
    vectors = Set(PropagationVector.NetworkExploit)
  ).toOption.get

  private val node = Node(
    NodeId.of("node-A").toOption.get,
    NodeType.Router,
    patchLevel = 0.2,
    defenseLevel = 0.5,
    state = NodeState.Healthy,
    workload = 0.3,
    Set()
  )

  private val topology = Topology(
    nodes = Map(node.nodeId.value -> node),
    edges = Set.empty
  )

  private val baseline = Scenario(
    name = "ScenarioIOUpdateSuite export target",
    topology = topology,
    virus = malware,
    startingNode = node,
    tick = 0,
    seed = 7,
    maxIterations = 3
  ).toOption.get

  private val jsonRepo = Repository.json[Scenario]

  test("ExportScenario(Json) writes model.currentScenario to disk, loadable back unchanged"):
    val state = AppState.init(ModelState(currentScenario = Some(baseline)))
    val path = ExportPaths.pathFor(baseline.name, FileFormat.Json)

    try
      val result = ScenarioIOUpdate.update(Msg.ExportScenario(FileFormat.Json), state)

      result.errors shouldBe Vector.empty
      Files.exists(path) shouldBe true
      jsonRepo.load(path) shouldBe Right(baseline)
    finally Files.deleteIfExists(path)

  test("ExportScenario(Txt) writes model.currentScenario to disk as text"):
    val state = AppState.init(ModelState(currentScenario = Some(baseline)))
    val path = ExportPaths.pathFor(baseline.name, FileFormat.Txt)

    try
      val result = ScenarioIOUpdate.update(Msg.ExportScenario(FileFormat.Txt), state)

      result.errors shouldBe Vector.empty
      Files.exists(path) shouldBe true
      Files.readString(path) should not be empty
    finally Files.deleteIfExists(path)

  test("ExportScenario reports an error when there is no scenario to export"):
    val state = AppState.init(ModelState())

    val result = ScenarioIOUpdate.update(Msg.ExportScenario(FileFormat.Json), state)

    result.errors should not be Vector.empty

  test("ImportScenario(Json) loads the scenario into model.currentScenario and scenarioForm"):
    val tempFile = Files.createTempFile("scenario-io-update-test", ".json")

    try
      jsonRepo.save(baseline, tempFile)
      val state = AppState.init(ModelState())

      val result = ScenarioIOUpdate.update(Msg.ImportScenario(FileFormat.Json, tempFile), state)

      result.model.currentScenario shouldBe Some(baseline)
      result.scenarioForm.map(_.name) shouldBe Some(baseline.name)
    finally Files.deleteIfExists(tempFile)

  test("ImportScenario(Json) reports an error when the file does not exist"):
    val state = AppState.init(ModelState())
    val missing = Path.of("path/does-not-exist.json")

    val result = ScenarioIOUpdate.update(Msg.ImportScenario(FileFormat.Json, missing), state)

    result.errors should not be Vector.empty
    result.model.currentScenario shouldBe None

  test("ImportScenario(Txt) reports that the format is not yet supported for import"):
    val state = AppState.init(ModelState())

    val result =
      ScenarioIOUpdate.update(Msg.ImportScenario(FileFormat.Txt, Path.of("whatever.txt")), state)

    result.errors should not be Vector.empty
    result.model.currentScenario shouldBe None
