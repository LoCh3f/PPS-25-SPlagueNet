package it.unibo.splague.simulation.event

import it.unibo.splague.model.Probability
import it.unibo.splague.model.countermeasures.{CountermeasureConfig, Countermeasures}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import it.unibo.splague.model.malware.MalwareKind.Worm
import it.unibo.splague.model.malware.{
  Malware,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import it.unibo.splague.simulation.Scenario
import it.unibo.splague.update.IsolationCriteria
import org.scalatest.matchers.should.Matchers.shouldBe

@RunWith(classOf[JUnitRunner])
class DefenseSuite extends AnyFunSuite:
  private val id1 = NodeId.of("node-01").getOrElse(fail())
  private val id2 = NodeId.of("node-02").getOrElse(fail())

  private val workstationNode =
    Node(id1, NodeType.Workstation, 0.0, 0.0, NodeState.Infected, 0.8, Set())
  private val serverNode = Node(id2, NodeType.Server, 0.0, 0.0, NodeState.Infected, 0.1, Set())

  private val topology = Topology(
    nodes = Map("node-01" -> workstationNode, "node-02" -> serverNode),
    edges = Set.empty
  )

  private val validTraits = (for
    infectivity <- Probability(0.5)
    stealth <- Probability(0.5)
    persistence <- Probability(0.5)
    footprint <- Probability(0.4)
  yield MalwareTraits(
    infectivity,
    stealth,
    payloadSeverity = PayloadSeverityLevel.Medium,
    persistence,
    footprint
  )).toOption.get

  private val dummyMalware = Malware(
    "isolator",
    Worm,
    validTraits,
    Set(PropagationVector.NetworkExploit)
  ).getOrElse(fail())

  test("Isolation event quarantines nodes matching the criteria when Isolation is active"):
    val config = CountermeasureConfig(
      activeCountermeasures = Set(Countermeasures.Isolation),
      countermeasureLevels = Map.empty,
      isolationCriteria = IsolationCriteria.byType(Set(NodeType.Workstation))
    ).getOrElse(fail())

    val scenario = Scenario(
      name = "Isolation Test",
      topology = topology,
      virus = dummyMalware,
      startingNode = workstationNode,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = config
    ).getOrElse(fail())

    val updatedScenario = Defense.IsolationEvent(scenario)

    updatedScenario.topology.nodes("node-01").state shouldBe NodeState.Quarantined
    updatedScenario.topology
      .nodes("node-02")
      .state shouldBe NodeState.Infected // unmatched node remains untouched

  test("Isolation event leaves nodes untouched when Isolation countermeasure is inactive"):
    val config = CountermeasureConfig(
      activeCountermeasures = Set.empty, // Isolation not active
      countermeasureLevels = Map.empty,
      isolationCriteria = IsolationCriteria.byType(Set(NodeType.Workstation))
    ).getOrElse(fail())

    val scenario = Scenario(
      name = "Inactive Isolation Test",
      topology = topology,
      virus = dummyMalware,
      startingNode = workstationNode,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = config
    ).getOrElse(fail())

    val updatedScenario = Defense.IsolationEvent(scenario)

    updatedScenario.topology.nodes("node-01").state shouldBe NodeState.Infected
    updatedScenario.topology.nodes("node-02").state shouldBe NodeState.Infected
