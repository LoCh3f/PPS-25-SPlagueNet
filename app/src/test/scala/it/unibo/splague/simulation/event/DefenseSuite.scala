package it.unibo.splague.simulation.event

import it.unibo.splague.model.Probability
import it.unibo.splague.model.connection.Connection.{Channel, ChannelType, Edge}
import it.unibo.splague.model.connection.Protocol.{
  ApplicationProtocol,
  ApplicationProtocolType,
  TcpTransport,
  TransportProtocol
}
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
import it.unibo.splague.update.{FirewallPolicy, IsolationCriteria}
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.not.contain
import org.scalatest.matchers.should.Matchers.{should, shouldBe}

private case class TestApplicationProtocol(
    kind: ApplicationProtocolType,
    underlying: TransportProtocol = TcpTransport
) extends ApplicationProtocol

@RunWith(classOf[JUnitRunner])
class DefenseSuite extends AnyFunSuite with Matchers with EitherValues:
  private val id1 =
    NodeId.of("node-01").fold(err => fail(s"Failed to create NodeId: $err"), identity)
  private val id2 =
    NodeId.of("node-02").fold(err => fail(s"Failed to create NodeId: $err"), identity)

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

  private def packetLoss(v: Double): Probability = Probability(v).fold(
    err => throw new IllegalStateException(s"Failed to define packet loss: $err"),
    v => v
  )

  private def channelOf(t: ChannelType): Channel =
    Channel(t, bandwidth = 100.0, latency = 10.0, jitter = 1.0, packetLoss = packetLoss(0.01))

  // Modifica protocolOf per istanziare la case class concreta
  private def protocolOf(k: ApplicationProtocolType): ApplicationProtocol =
    TestApplicationProtocol(kind = k)

  private val wanHttpsEdge = Edge(
    workstationNode,
    serverNode,
    channelOf(ChannelType.WAN),
    Some(protocolOf(ApplicationProtocolType.HTTPS))
  )
  private val lanFtpEdge = Edge(
    workstationNode,
    serverNode,
    channelOf(ChannelType.LAN),
    Some(protocolOf(ApplicationProtocolType.FTP))
  )
  private val lanHttpsEdge = Edge(
    workstationNode,
    serverNode,
    channelOf(ChannelType.LAN),
    Some(protocolOf(ApplicationProtocolType.HTTPS))
  )
  private val vpnFtpEdge = Edge(
    workstationNode,
    serverNode,
    channelOf(ChannelType.VPN),
    Some(protocolOf(ApplicationProtocolType.FTP))
  )

  private def scenarioWith(config: CountermeasureConfig, edges: Set[Edge]): Scenario =
    Scenario(
      name = "Firewall Test",
      topology = topology.copy(edges = edges),
      virus = dummyMalware,
      startingNode = workstationNode,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = config
    ).getOrElse(fail())

  test("Isolation event quarantines nodes matching the criteria when Isolation is active"):
    val config = CountermeasureConfig(
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

  // --- Firewall ---

  test("Firewall event blocks edges with WAN channel type and FTP application protocol"):
    val config =
      CountermeasureConfig().getOrElse(fail())
    val scenario = scenarioWith(config, Set(wanHttpsEdge, lanFtpEdge))

    val updatedScenario = Defense.FirewallEvent(scenario)

    FirewallPolicy.isBlocked(
      wanHttpsEdge,
      updatedScenario.countermeasureConfig.firewallPolicy
    ) shouldBe true
    FirewallPolicy.isBlocked(
      lanFtpEdge,
      updatedScenario.countermeasureConfig.firewallPolicy
    ) shouldBe true

  test("Firewall event does not block legitimate LAN/HTTPS traffic"):
    val config =
      CountermeasureConfig().getOrElse(fail())
    val scenario = scenarioWith(config, Set(lanHttpsEdge))

    val updatedScenario = Defense.FirewallEvent(scenario)

    FirewallPolicy.isBlocked(
      lanHttpsEdge,
      updatedScenario.countermeasureConfig.firewallPolicy
    ) shouldBe false

  test("Firewall event never blocks VPN traffic, regardless of the application protocol"):
    val config =
      CountermeasureConfig(activeCountermeasures = Set(Countermeasures.Firewall)).getOrElse(fail())
    val scenario = scenarioWith(config, Set(vpnFtpEdge))

    val updatedScenario = Defense.FirewallEvent(scenario)

    FirewallPolicy.isBlocked(
      vpnFtpEdge,
      updatedScenario.countermeasureConfig.firewallPolicy
    ) shouldBe false

  test("Firewall event cuts blocked edges from the scenario topology"):
    val config = CountermeasureConfig().getOrElse(fail())
    // wanHttpsEdge is blocked by default, lanHttpsEdge no
    val wanFtpEdge = Edge(
      workstationNode,
      serverNode,
      channelOf(ChannelType.WAN),
      Some(protocolOf(ApplicationProtocolType.FTP))
    )
    val scenario = scenarioWith(config, Set(wanHttpsEdge, lanHttpsEdge))

    val updatedScenario = Defense.FirewallEvent(scenario)

    updatedScenario.topology.edges should contain(lanHttpsEdge)
    updatedScenario.topology.edges should not contain (wanHttpsEdge)
