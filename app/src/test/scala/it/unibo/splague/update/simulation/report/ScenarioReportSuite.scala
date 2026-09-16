package it.unibo.splague.update.simulation.report

import it.unibo.splague.dsl.*
import it.unibo.splague.model.connection.Connection.ChannelType.LAN
import it.unibo.splague.model.countermeasures.{CountermeasureConfig, Countermeasures}
import it.unibo.splague.model.malware.*
import it.unibo.splague.model.node.NodeType.Workstation
import it.unibo.splague.model.{Probability, Scenario}
import it.unibo.splague.update.simulation.event.SimulationEvents.{Event, EventSelector}
import it.unibo.splague.update.simulation.report.ScenarioReport
import org.junit.runner.RunWith
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class ScenarioReportSuite extends AnyFunSuite with Matchers with EitherValues:

  /** Never mutates the scenario: isolates report plumbing (tick coverage, identity fields,
    * activation observation) from simulation dynamics, which are already covered elsewhere.
    */
  private val noOpEvent: Event = (s: Scenario) => s
  private val noOpSelector: EventSelector = (_: Scenario) => noOpEvent

  private def initialScenario(maxIterations: Int): Scenario =
    val topologyResult = topology:
      node("A", Workstation)
      node("B", Workstation)
      "A" <-> "B" via LAN

    val builtTopology = topologyResult.getOrElse(fail("Failed to build topology"))

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

    Scenario(
      name = "report-test-scenario",
      topology = builtTopology,
      virus = malware,
      startingNode = builtTopology.nodes("A"),
      tick = 0,
      seed = 7,
      maxIterations = maxIterations,
      countermeasureConfig = countermeasureConfig
    ).getOrElse(fail("Failed to create Scenario"))

  test(
    "ScenarioReport.from carries scenario identity and covers every tick from 0 to maxIterations"
  ):
    val initial = initialScenario(maxIterations = 5)
    val report = ScenarioReport.from(initial, noOpSelector)

    report.scenarioName shouldBe "report-test-scenario"
    report.seed shouldBe 7
    report.malwareName shouldBe "TestWorm"
    report.timeline.map(_.tick) shouldBe (0 to 5).toVector
    report.finalTick.tick shouldBe 5

  test("ScenarioReport.from records a pre-activated countermeasure as active from tick 0"):
    val initial = initialScenario(maxIterations = 5)
    val report = ScenarioReport.from(initial, noOpSelector)

    report.activationTicks shouldBe Map(Countermeasures.Patch -> 0)

  test("ScenarioReport.from does not record a countermeasure that never activated"):
    val initial = initialScenario(maxIterations = 5)
    val report = ScenarioReport.from(initial, noOpSelector)

    report.activationTicks.keySet should not contain Countermeasures.Firewall

  test("ScenarioReport.from is deterministic for the same initial scenario and selector"):
    val initial = initialScenario(maxIterations = 5)

    ScenarioReport.from(initial, noOpSelector) shouldBe ScenarioReport.from(initial, noOpSelector)
