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
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import it.unibo.splague.simulation.Scenario
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PreventionSuite extends AnyFunSuite:
  private val nodeId1 = NodeId.of("n1").getOrElse(fail("Failed to create NodeId"))

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

  private val baseNode = Node(
    nodeId = nodeId1,
    nodeType = NodeType.Workstation,
    patchLevel = 0.0,
    defenseLevel = 0.2,
    state = NodeState.Healthy,
    workload = 0.0,
    vectors = Set()
  )

  private val topology = Topology(Map("n1" -> baseNode), Set())

  test("Prevention event applies defense updates when DefenseBoost countermeasure is active"):
    val config = CountermeasureConfig(
      activeCountermeasures = Set(Countermeasures.DefenseBoost),
      countermeasureLevels = Map(0.0 -> Countermeasures.DefenseBoost)
    ).getOrElse(fail("Failed to create config"))

    val scenario = Scenario(
      name = "Prevention Test",
      topology = topology,
      virus = dummyVirus,
      startingNode = baseNode,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = config
    ).getOrElse(fail("Failed to create scenario"))

    val updatedScenario = Prevention.Prevention(scenario)
    val updatedNode = updatedScenario.topology.nodes("n1")

    updatedNode.defenseLevel should be > baseNode.defenseLevel

  test("Prevention event applies patch updates when Patch countermeasure is active"):
    val config = CountermeasureConfig(
      activeCountermeasures = Set(Countermeasures.Patch),
      countermeasureLevels = Map(0.0 -> Countermeasures.Patch)
    ).getOrElse(fail("Failed to create config"))

    val scenario = Scenario(
      name = "Prevention Test",
      topology = topology,
      virus = dummyVirus,
      startingNode = baseNode,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = config
    ).getOrElse(fail("Failed to create scenario"))

    val updatedScenario = Prevention.Prevention(scenario)
    val updatedNode = updatedScenario.topology.nodes("n1")

    updatedNode.patchLevel should be > baseNode.patchLevel

  test("Prevention event leaves topology unchanged when no countermeasure is inactive"):
    val scenario = Scenario(
      name = "No Prevention Test",
      topology = topology,
      virus = dummyVirus,
      startingNode = baseNode,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = CountermeasureConfig.empty
    ).getOrElse(fail("Failed to create scenario"))

    val updatedScenario = Prevention.Prevention(scenario)

    updatedScenario.topology.nodes("n1").defenseLevel shouldBe baseNode.defenseLevel
