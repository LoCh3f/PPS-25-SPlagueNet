package it.unibo.splague.dsl

import it.unibo.splague.model.Scenario
import it.unibo.splague.model.connection.Connection.ChannelType.LAN
import it.unibo.splague.model.countermeasures.CountermeasureConfig
import it.unibo.splague.model.malware.MalwareKind.Worm
import it.unibo.splague.model.malware.PayloadSeverityLevel.Low
import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.model.malware.PropagationVector.NetworkExploit
import it.unibo.splague.model.node.NodeType.{Router, Workstation}
import it.unibo.splague.utils.config.SimulationConfig
import org.junit.runner.RunWith
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

/** Add the tests one at a time, in this order: each one is a red/green step that only needs the
  * smallest extension of `ScenarioBuilder` to pass.
  */
@RunWith(classOf[JUnitRunner])
final class ScenarioDslSuite extends AnyFunSuite with Matchers with EitherValues:

  private def declareWorm(
      infectivity: Double = 0.5,
      vectors: Set[PropagationVector] = Set(NetworkExploit)
  )(using ScenarioBuilder): Unit =
    malware(
      "TestWorm",
      Worm,
      infectivity = infectivity,
      stealth = 0.5,
      severity = Low,
      persistence = 0.5,
      footprint = 0.5,
      vectors = vectors
    )

  private def minimalScenario(): ValidationResult[Scenario] =
    scenario("Outbreak"):
      network:
        node("A", Workstation)
      declareWorm()
      startingNode("A")

  test("a complete declaration builds a scenario with the declared name, topology and malware"):
    val result = scenario("Outbreak", seedSource = 1):
      network:
        node("A", Workstation)
        node("B", Workstation)
        "A" <-> "B" via LAN
      declareWorm()
      startingNode("A")

    val built = result.value
    built.name shouldBe "Outbreak"
    built.topology.nodes.keys should contain allOf ("A", "B")
    built.topology.edges.size shouldBe 1
    built.virus.name shouldBe "TestWorm"
    built.startingNode.nodeId.value shouldBe "A"
    built.tick shouldBe 0

  test("the starting node id is normalized the same way node ids are"):
    val result = scenario("Outbreak"):
      network:
        node("A", Workstation)
      declareWorm()
      startingNode(" A ")

    result.value.startingNode.nodeId.value shouldBe "A"

  test("a shape declared in the network block can be referenced as the starting node"):
    val result = scenario("Outbreak"):
      network:
        star("hub", Router, "leaf", 3, Workstation, LAN)
      declareWorm()
      startingNode("hub")

    val built = result.value
    built.topology.nodes.size shouldBe 4
    built.startingNode.nodeId.value shouldBe "hub"

  test("an unspecified seed is taken from the seed source"):
    val result = scenario("Outbreak", seedSource = 99):
      network:
        node("A", Workstation)
      declareWorm()
      startingNode("A")

    result.value.seed shouldBe 99

  test("an explicit seed overrides the seed source"):
    val result = scenario("Outbreak", seedSource = 99):
      network:
        node("A", Workstation)
      declareWorm()
      startingNode("A")
      seed(7)

    result.value.seed shouldBe 7

  test("the default seed source never produces a negative seed"):
    val seeds = List.fill(50)(minimalScenario().value.seed)

    seeds.foreach(seed => seed should be >= 0)

  test("an unspecified max iterations falls back to the configured default"):
    minimalScenario().value.maxIterations shouldBe SimulationConfig.Defaults.MAX_ITERATIONS

  test("an explicit max iterations overrides the default"):
    val result = scenario("Outbreak"):
      network:
        node("A", Workstation)
      declareWorm()
      startingNode("A")
      maxIterations(12)

    result.value.maxIterations shouldBe 12

  test("countermeasures default to the empty configuration when not declared"):
    minimalScenario().value.countermeasureConfig shouldBe CountermeasureConfig.empty

  test("a scenario that declares nothing reports every missing piece"):
    val result = scenario("Empty"):
      ()

    result.left.value should contain allOf (
      "The scenario must declare a network",
      "The scenario must declare a malware",
      "The scenario must declare a starting node"
    )

  test("a starting node missing from the topology is reported as an unknown node id"):
    val result = scenario("Outbreak"):
      network:
        node("A", Workstation)
      declareWorm()
      startingNode("ghost")

    result.left.value should contain("Starting node references unknown node id: ghost")

  test("topology errors accumulate with scenario-level errors without cascading"):
    val result = scenario("Outbreak"):
      network:
        node("bad id", Workstation)
      startingNode("A")

    val errors = result.left.value
    errors should contain("The ID cannot contain white space")
    errors should contain("The scenario must declare a malware")
    errors should not contain "Starting node references unknown node id: A"

  test("a malware probability outside [0,1] is reported as an error instead of being clamped"):
    val result = scenario("Outbreak"):
      network:
        node("A", Workstation)
      declareWorm(infectivity = 1.5)
      startingNode("A")

    result.left.value should contain("infectivity: Probability must be in [0,1], got 1.5")

  test("a malware without propagation vectors reports the smart constructor's error"):
    val result = scenario("Outbreak"):
      network:
        node("A", Workstation)
      declareWorm(vectors = Set.empty)
      startingNode("A")

    result.left.value should contain("Malware must declare at least one propagation vector")

  test("declaring the starting node twice is reported as an error"):
    val result = scenario("Outbreak"):
      network:
        node("A", Workstation)
        node("B", Workstation)
      declareWorm()
      startingNode("A")
      startingNode("B")

    result.left.value should contain("The starting node is declared more than once")
