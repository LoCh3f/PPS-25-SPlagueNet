package it.unibo.splague.simulation.event

import it.unibo.splague.model.Probability
import it.unibo.splague.model.malware.{
  Malware,
  MalwareKind,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import it.unibo.splague.model.connection.Connection
import it.unibo.splague.simulation.Scenario
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class InfectionSuite extends AnyFunSuite:

  private val idealTraits = (for
    infectivity <- Probability(1.0)
    stealth <- Probability(0.0)
    persistence <- Probability(0.0)
    footprint <- Probability(0.0)
  yield MalwareTraits(
    infectivity,
    stealth,
    PayloadSeverityLevel.Low,
    persistence,
    footprint
  )).toOption.get

  private val zeroTraits = (for
    infectivity <- Probability(0.0)
    stealth <- Probability(0.0)
    persistence <- Probability(0.0)
    footprint <- Probability(0.0)
  yield MalwareTraits(
    infectivity,
    stealth,
    PayloadSeverityLevel.Low,
    persistence,
    footprint
  )).toOption.get

  private val malwareAlways = Malware(
    "all",
    MalwareKind.Worm,
    idealTraits,
    Set(PropagationVector.NetworkExploit)
  ).toOption.get
  private val malwareNever = Malware(
    "none",
    MalwareKind.Worm,
    zeroTraits,
    Set(PropagationVector.NetworkExploit)
  ).toOption.get

  private val nodeInfected = Node(
    NodeId.of("n-inf").getOrElse(fail("invalid id")),
    NodeType.Router,
    0.0,
    0.0,
    NodeState.Infected,
    0.0,
    Set()
  )
  private val nodeHealthy = Node(
    NodeId.of("n-hlth").getOrElse(fail("invalid id")),
    NodeType.Workstation,
    0.0,
    0.0,
    NodeState.Healthy,
    0.0,
    Set()
  )
  private val nodeHealthy1 = Node(
    NodeId.of("n-hlth-2").getOrElse(fail("invalid id")),
    NodeType.Server,
    0.0,
    0.0,
    NodeState.Healthy,
    0.0,
    Set()
  )
  private val nodeHealthyIndirect = Node(
    NodeId.of("n-hlth-3").getOrElse(fail("invalid id")),
    NodeType.MobileDevice,
    0.0,
    0.0,
    NodeState.Healthy,
    0.0,
    Set()
  )

  private val baseChannel =
    Connection.Channel(Connection.ChannelType.LAN, 100.0, 1.0, 0.1, Probability.clamped(0.0))

  test("InfectionEvent should infect healthy neighbors when infection probability is 1.0"):
    val topo = Topology(
      nodes = Map(
        nodeInfected.nodeId.value -> nodeInfected,
        nodeHealthy.nodeId.value -> nodeHealthy,
        nodeHealthy1.nodeId.value -> nodeHealthy1,
        nodeHealthyIndirect.nodeId.value -> nodeHealthyIndirect
      ),
      edges = Set(
        Connection.Edge(nodeInfected, nodeHealthy, baseChannel, None),
        Connection.Edge(nodeInfected, nodeHealthy1, baseChannel, None),
        Connection.Edge(nodeHealthy, nodeHealthyIndirect, baseChannel, None)
      )
    )

    val scenario = Scenario(
      "s",
      topo,
      malwareAlways,
      nodeInfected,
      tick = 0,
      seed = 0,
      maxIterations = 1
    ).toOption.get

    val result = Infection.InfectionEvent.apply(scenario)
    result.topology.nodes(nodeHealthy.nodeId.value).state shouldBe NodeState.Infected
    result.topology.nodes(nodeHealthy1.nodeId.value).state shouldBe NodeState.Infected
    result.topology.nodes(nodeHealthyIndirect.nodeId.value).state shouldBe NodeState.Healthy

  test("InfectionEvent should NOT infect when infection probability is 0.0"):
    val topo = Topology(
      nodes = Map(
        nodeInfected.nodeId.value -> nodeInfected,
        nodeHealthy.nodeId.value -> nodeHealthy,
        nodeHealthy1.nodeId.value -> nodeHealthy1,
        nodeHealthyIndirect.nodeId.value -> nodeHealthyIndirect
      ),
      edges = Set(
        Connection.Edge(nodeInfected, nodeHealthy, baseChannel, None),
        Connection.Edge(nodeInfected, nodeHealthy1, baseChannel, None),
        Connection.Edge(nodeHealthy, nodeHealthyIndirect, baseChannel, None)
      )
    )

    val scenario = Scenario(
      "s",
      topo,
      malwareNever,
      nodeInfected,
      tick = 0,
      seed = 0,
      maxIterations = 1
    ).toOption.get

    val result = Infection.InfectionEvent.apply(scenario)

    result.topology.nodes(nodeHealthy.nodeId.value).state shouldBe NodeState.Healthy
    result.topology.nodes(nodeHealthy1.nodeId.value).state shouldBe NodeState.Healthy
    result.topology.nodes(nodeHealthyIndirect.nodeId.value).state shouldBe NodeState.Healthy
