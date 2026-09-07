package it.unibo.splague.simulation.event

import it.unibo.splague.model.Awareness
import it.unibo.splague.model.Probability
import it.unibo.splague.model.malware.{
  Malware,
  MalwareKind,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import it.unibo.splague.simulation.Scenario
import it.unibo.splague.update.AwarenessRules
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class DetectionSuite extends AnyFunSuite with Matchers:

  private val traits = (for
    infectivity <- Probability(0.5)
    stealth <- Probability(0.2)
    persistence <- Probability(0.5)
    footprint <- Probability(0.3)
  yield MalwareTraits(
    infectivity,
    stealth,
    PayloadSeverityLevel.Low,
    persistence,
    footprint
  )).toOption.get

  private val malware =
    Malware("m", MalwareKind.Worm, traits, Set(PropagationVector.NetworkExploit)).toOption.get

  private val node = Node(
    NodeId.of("n1").toOption.get,
    NodeType.Server,
    patchLevel = 0.0,
    defenseLevel = 0.0,
    NodeState.Healthy,
    workload = 0.6,
    Set()
  )

  private val topology = Topology(nodes = Map(node.nodeId.value -> node), edges = Set.empty)

  private def freshScenario(): Scenario =
    Scenario(
      name = "s",
      topology = topology,
      virus = malware,
      startingNode = node,
      tick = 0,
      seed = 0,
      maxIterations = 10
    ).toOption.get

  test("a freshly created scenario starts with Awareness.none"):
    freshScenario().awareness shouldBe Awareness.none

  test("Detection.apply should raise scenario awareness by the computed detection signal"):
    val scenario = freshScenario()
    val expectedSignal = AwarenessRules.detectionSignal(scenario.topology, scenario.virus)

    val result = Detection.apply(scenario)

    result.awareness.value shouldBe (scenario.awareness.value + expectedSignal) +- 0.0001

  test("Detection.apply should leave topology and other scenario fields unchanged"):
    val scenario = freshScenario()

    val result = Detection.apply(scenario)

    result.topology shouldBe scenario.topology
    result.tick shouldBe scenario.tick
    result.seed shouldBe scenario.seed
    result.name shouldBe scenario.name
    result.virus shouldBe scenario.virus

  test("Detection.apply repeated over many ticks should accumulate awareness, clamped at 1.0"):
    val scenario = freshScenario()

    val result = (1 to 50).foldLeft(scenario)((s, _) => Detection.apply(s))

    result.awareness.value shouldBe 1.0

  test("Detection.apply on an all-idle topology (zero workload) should not raise awareness"):
    val idleNode = node.copy(workload = 0.0)
    val idleTopology = Topology(nodes = Map(idleNode.nodeId.value -> idleNode), edges = Set.empty)
    val scenario = Scenario(
      name = "idle",
      topology = idleTopology,
      virus = malware,
      startingNode = idleNode,
      tick = 0,
      seed = 0,
      maxIterations = 10
    ).toOption.get

    val result = Detection.apply(scenario)

    result.awareness shouldBe Awareness.none
