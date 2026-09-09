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
import org.scalactic.Tolerance.convertNumericToPlusOrMinusWrapper
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
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

  private val recoveryNodeId = NodeId.of("node-03").getOrElse(fail())
  private val restingWorkload = 0.2
  private val elevatedWorkload = 0.6

  private val recoveryNodeAtRest =
    Node(recoveryNodeId, NodeType.Server, 0.5, 0.5, NodeState.Infected, restingWorkload, Set())

  private val topologyForRecovery =
    Topology(nodes = Map("node-03" -> recoveryNodeAtRest), edges = Set.empty)

  private def scenarioWithNode(node: Node, topology: Topology): Scenario =
    Scenario(
      name = "Recovery Test",
      topology = topology,
      virus = dummyVirus,
      startingNode = node,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = config
    ).getOrElse(fail("Failed to create scenario"))

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

  test(
    "LowerWorkloadEvent decreases the workload of an Immune node, without dropping below its baseline"
  ):
    val baseScenario = scenarioWithNode(recoveryNodeAtRest, topologyForRecovery)

    val elevatedNode =
      recoveryNodeAtRest.copy(state = NodeState.Immune, workload = elevatedWorkload)
    val scenario =
      baseScenario.copy(topology = topologyForRecovery.copy(nodes = Map("node-03" -> elevatedNode)))

    val updated = Cure.LowerWorkloadEvent(scenario)
    val updatedWorkload = updated.topology.nodes("node-03").workload

    updatedWorkload should be < elevatedWorkload
    updatedWorkload should be >= restingWorkload

//  test("LowerWorkloadEvent does not go below the node's baseline workload"):
//    val baseScenario = scenarioWithNode(recoveryNodeAtRest, topologyForRecovery)
//
//    val atBaselineNode = recoveryNodeAtRest.copy(state = NodeState.Immune, workload = restingWorkload)
//    val scenario = baseScenario.copy(topology = topologyForRecovery.copy(nodes = Map("node-03" -> atBaselineNode)))
//
//    val updated = Cure.LowerWorkloadEvent(scenario)
//
//    updated.topology.nodes("node-03").workload shouldBe (restingWorkload) +- 0.0001

  test("LowerWorkloadEvent leaves non-Immune nodes untouched"):
    val elevatedInfected = nodeHighPatch.copy(workload = 0.7)
    val topology = topologyHighPatch.copy(nodes = Map("node-01" -> elevatedInfected))
    val scenario = scenarioWithNode(elevatedInfected, topology)

    val updated = Cure.LowerWorkloadEvent(scenario)

    updated.topology.nodes("node-01").workload shouldBe 0.7
