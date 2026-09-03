package it.unibo.splague.simulation.event

import it.unibo.splague.model.Probability
import it.unibo.splague.model.countermeasures.{CountermeasureConfig, Countermeasures}
import it.unibo.splague.model.malware.MalwareKind.Worm
import it.unibo.splague.model.malware.{
  Malware,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.NodeState.{Immune, Infected}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import it.unibo.splague.simulation.Scenario
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CureSuite extends AnyFunSuite:
  private val id1 = NodeId.of("node-01").getOrElse(fail())
  private val id2 = NodeId.of("node-02").getOrElse(fail())

  private val nodeHighPatch = Node(id1, NodeType.Router, 0.8, 0.2, NodeState.Infected, 0.0, Set())
  private val nodeLowPatch = Node(id2, NodeType.Server, 0.0, 0.1, NodeState.Infected, 0.0, Set())
  private val nodeQuarantined =
    Node(id1, NodeType.Router, 0.8, 0.2, NodeState.Quarantined, 0.0, Set())

  private val topologyHighPatch = Topology(
    nodes = Map("node-01" -> nodeHighPatch),
    edges = Set.empty
  )
  private val topologyLowPatch = Topology(
    nodes = Map("node-02" -> nodeLowPatch),
    edges = Set.empty
  )
  private val topologyQuarantined =
    Topology(nodes = Map("node-01" -> nodeQuarantined), edges = Set.empty)

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

  private val dummyVirus = Malware(
    "dummy",
    Worm,
    validTraits,
    vectors = Set(PropagationVector.NetworkExploit)
  ).toOption.get

  private val config = CountermeasureConfig(
    activeCountermeasures = Set(Countermeasures.Patch),
    countermeasureLevels = Map(0.0 -> Countermeasures.Patch)
  ).toOption.get

  test("Cure event heals an Infected node with high patch level"):
    val scenario = Scenario(
      name = "Prevention Test",
      topology = topologyHighPatch,
      virus = dummyVirus,
      startingNode = nodeHighPatch,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = config
    ).getOrElse(fail("Failed to create scenario"))

    val updatedScenario = Cure.CureEvent(scenario)
    val updatedNode = updatedScenario.topology.nodes("node-01")

    updatedNode.state shouldBe Immune

  test("Cure event cannot heal an Infected node with a low patch level"):
    val scenario = Scenario(
      name = "Prevention Test",
      topology = topologyLowPatch,
      virus = dummyVirus,
      startingNode = nodeLowPatch,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = config
    ).getOrElse(fail("Failed to create scenario"))

    val updatedScenario = Cure.CureEvent(scenario)
    val updatedNode = updatedScenario.topology.nodes("node-02")

    updatedNode.state shouldBe Infected

  test("Cure event heals a Quarantined node with high patch level"):
    val scenario = Scenario(
      name = "Quarantine Cure Test",
      topology = topologyQuarantined,
      virus = dummyVirus,
      startingNode = nodeQuarantined,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = config
    ).getOrElse(fail("Failed to create scenario"))

    val updatedScenario = Cure.CureEvent(scenario)
    updatedScenario.topology.nodes("node-01").state shouldBe Immune

  test("Cure event leaves Infected nodes unchanged when Patch countermeasure is inactive"):
    val inactiveConfig = CountermeasureConfig(
      activeCountermeasures = Set.empty,
      countermeasureLevels = Map.empty
    ).toOption.get

    val scenario = Scenario(
      name = "Inactive Patch Test",
      topology = topologyHighPatch,
      virus = dummyVirus,
      startingNode = nodeHighPatch,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = inactiveConfig
    ).getOrElse(fail("Failed to create scenario"))

    val updatedScenario = Cure.CureEvent(scenario)
    updatedScenario.topology.nodes("node-01").state shouldBe Infected
