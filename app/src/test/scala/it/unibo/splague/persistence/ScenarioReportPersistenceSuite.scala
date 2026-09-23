package it.unibo.splague.persistence

import it.unibo.splague.dsl.*
import it.unibo.splague.model.connection.Connection.ChannelType.LAN
import it.unibo.splague.model.countermeasures.CountermeasureConfig
import it.unibo.splague.model.malware.MalwareKind.Worm
import it.unibo.splague.model.malware.{
  Malware,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.NodeType.Workstation
import it.unibo.splague.model.{Probability, Scenario}
import it.unibo.splague.update.simulation.event.SimulationEvents.{Event, EventSelector}
import it.unibo.splague.update.simulation.report.ScenarioReport
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import java.nio.file.{Files, Path}

@RunWith(classOf[JUnitRunner])
final class ScenarioReportPersistenceSuite extends AnyFunSuite with Matchers:

  import it.unibo.splague.persistence.codecs.json.CodecCatalog.given
  import it.unibo.splague.persistence.codecs.json.JsonCodec.given

  private val noOpEvent: Event = (s: Scenario) => s
  private val noOpSelector: EventSelector = (_: Scenario) => noOpEvent

  private def defaultReport(): ScenarioReport =
    val builtTopology = (topology:
      node("A", Workstation)
      node("B", Workstation)
      "A" <-> "B" via LAN
    )
      .getOrElse(fail())

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

    val scenario = Scenario(
      name = "Full Simulation Test",
      topology = builtTopology,
      virus = dummyVirus,
      startingNode = builtTopology.nodes("A"),
      tick = 0,
      seed = 123,
      maxIterations = 5,
      countermeasureConfig = CountermeasureConfig.empty
    ).getOrElse(fail())

    ScenarioReport.from(scenario, noOpSelector)

  private val repo = Repository.json[ScenarioReport]

  test("saving and loading a ScenarioReport as JSON round-trips to an equal value"):
    val tempFile = Files.createTempFile("scenario-report-test", ".json")
    val report = defaultReport()

    try
      val saved = repo.save(report, tempFile)
      assert(saved.isRight)

      val loaded = repo.load(tempFile)
      loaded shouldBe Right(report)
    finally Files.deleteIfExists(tempFile)

  test("loading a ScenarioReport from a non-existent path returns a PersistenceError.IO"):
    val result = repo.load(Path.of("path/fake-report.json"))

    result match
      case Left(PersistenceError.IO(_)) => succeed
      case other                        => fail(s"expected PersistenceError.IO, got $other")

  test("loading malformed ScenarioReport JSON returns a PersistenceError.Parsing"):
    val tempFile = Files.createTempFile("malformed-report", ".json")
    Files.writeString(tempFile, "{ not a valid json ")

    try
      val result = repo.load(tempFile)
      result match
        case Left(PersistenceError.Parsing(_)) => succeed
        case other => fail(s"expected PersistenceError.Parsing, obtained $other")
    finally Files.deleteIfExists(tempFile)
