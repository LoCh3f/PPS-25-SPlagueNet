package it.unibo.splague.simulation.event

import it.unibo.splague.model.Probability
import it.unibo.splague.model.countermeasures.CountermeasureConfig
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
import org.scalatest.matchers.should.Matchers.*
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DestroySuite extends AnyFunSuite:

  private val id1 = NodeId.of("node-01").getOrElse(fail())
  private val id2 = NodeId.of("node-02").getOrElse(fail())

  private val infectedNode =
    Node(id1, NodeType.Workstation, 0.0, 0.0, NodeState.Infected, 0.8, Set())
  private val healthyNode = Node(id2, NodeType.Workstation, 0.0, 0.0, NodeState.Healthy, 0.1, Set())

  private val topology = Topology(
    nodes = Map("node-01" -> infectedNode, "node-02" -> healthyNode),
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
    "destroyer",
    Worm,
    validTraits,
    Set(PropagationVector.NetworkExploit)
  ).getOrElse(fail())

  test("Destroy event transitions heavily loaded infected nodes to Destroyed state"):
    val scenario = Scenario(
      name = "Destroy Test",
      topology = topology,
      virus = dummyMalware,
      startingNode = infectedNode,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = CountermeasureConfig.empty
    ).getOrElse(fail())

    val updatedScenario = Destroy.DestroyEvent(scenario)

    // Infected node gets workload increased and should pass destruction threshold depending on RNG seed
    updatedScenario.topology.nodes("node-01").state shouldBe NodeState.Destroyed

  test("Destroy event leaves healthy or non-infected nodes completely untouched"):
    val scenario = Scenario(
      name = "Destroy Ignore Healthy Test",
      topology = topology,
      virus = dummyMalware,
      startingNode = infectedNode,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = CountermeasureConfig.empty
    ).getOrElse(fail())

    val updatedScenario = Destroy.DestroyEvent(scenario)

    val healthyAfter = updatedScenario.topology.nodes("node-02")
    healthyAfter.state shouldBe NodeState.Healthy
    healthyAfter.workload shouldBe healthyNode.workload
