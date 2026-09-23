package it.unibo.splague.persistence

import it.unibo.splague.dsl.*
import it.unibo.splague.model.connection.Connection.ChannelType.LAN
import it.unibo.splague.model.countermeasures.{CountermeasureConfig, Countermeasures}
import it.unibo.splague.model.malware.*
import it.unibo.splague.model.node.NodeType.Workstation
import it.unibo.splague.model.{Probability, Scenario}
import it.unibo.splague.persistence.codecs.json.CodecCatalog.given
import it.unibo.splague.persistence.codecs.json.JsonCodec.given
import it.unibo.splague.update.simulation.event.SimulationEvents.{Event, EventSelector}
import it.unibo.splague.update.simulation.report.ScenarioReport
import org.junit.runner.RunWith
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import java.nio.file.Files

@RunWith(classOf[JUnitRunner])
final class ScenarioReportPersistenceSuite extends AnyFunSuite with Matchers with EitherValues:

  private val noOpEvent: Event = (s: Scenario) => s
  private val noOpSelector: EventSelector = (_: Scenario) => noOpEvent

  private def sampleReport(): ScenarioReport =
    val builtTopology = (topology:
      node("A", Workstation)
      node("B", Workstation)
      "A" <-> "B" via LAN
    )
      .getOrElse(fail("Failed to build topology"))

    val malware = Malware(
      name = "TestWorm",
      kind = MalwareKind.Worm,
      traits = MalwareTraits(
        infectivity = Probability.clamped(0.5),
        stealth = Probability.clamped(0.5),
        payloadSeverity = PayloadSeverityLevel.Low,
        persistence = Probability.clamped(0.5),
        footprint = Probability.clamped(0.5)
      ),
      vectors = Set(PropagationVector.NetworkExploit)
    ).getOrElse(fail("Failed to create Malware"))

    val countermeasureConfig = CountermeasureConfig(
      activeCountermeasures = Set(Countermeasures.Patch)
    ).getOrElse(fail("Failed to create CountermeasureConfig"))

    val initial = Scenario(
      name = "persistence-test-scenario",
      topology = builtTopology,
      virus = malware,
      startingNode = builtTopology.nodes("A"),
      tick = 0,
      seed = 7,
      maxIterations = 5,
      countermeasureConfig = countermeasureConfig
    ).getOrElse(fail("Failed to create Scenario"))

    ScenarioReport.from(initial, noOpSelector)

  test("a saved ScenarioReport can be loaded back unchanged"):
    val report = sampleReport()
    val repo = Repository.json[ScenarioReport]
    val path = Files.createTempFile("scenario-report", ".json")

    try
      val roundTripped =
        for
          _ <- repo.save(report, path)
          loaded <- repo.load(path)
        yield loaded

      roundTripped.value shouldBe report
    finally Files.deleteIfExists(path)
