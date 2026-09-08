package it.unibo.splague.update

import it.unibo.splague.model.connection.Connection.*
import it.unibo.splague.model.connection.Protocol.*
import it.unibo.splague.model.node.{Node, NodeId, NodeType}
import it.unibo.splague.model.Probability
import it.unibo.splague.model.connection.Protocol
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FirewallPolicySuite extends AnyFunSuite:

  val dummySourceId = NodeId
    .of("node-1")
    .fold(
      err => throw new IllegalStateException(s"Failed to create test NodeId: $err"),
      id => id
    )
  val dummyTargetId = NodeId
    .of("node-2")
    .fold(
      err => throw new IllegalStateException(s"Failed to create test NodeId: $err"),
      id => id
    )
  val dummySource = Node.test(dummySourceId, NodeType.Server)
  val dummyTarget = Node.test(dummyTargetId, NodeType.Server)

  val lanChannel = Channel(ChannelType.LAN, 1000.0, 5.0, 0.1, Probability.apply(0.0).toOption.get)
  val wanChannel = Channel(ChannelType.WAN, 100.0, 50.0, 2.0, Probability.apply(0.0).toOption.get)
  val vpnChannel = Channel(ChannelType.VPN, 500.0, 30.0, 1.0, Probability.apply(0.0).toOption.get)

  private val httpProtocol: Protocol.ApplicationProtocol = new Protocol.ApplicationProtocol:
    override def kind: Protocol.ApplicationProtocolType = Protocol.ApplicationProtocolType.HTTP
    override def underlying: Protocol.TransportProtocol = Protocol.TcpTransport

  private val ftpProtocol: Protocol.ApplicationProtocol = new Protocol.ApplicationProtocol:
    override def kind: Protocol.ApplicationProtocolType = Protocol.ApplicationProtocolType.FTP
    override def underlying: Protocol.TransportProtocol = Protocol.TcpTransport

  test("An edge with unblocked channel and safe protocol passes inspection") {
    val policy = FirewallPolicy(
      blockedChannels = Set(ChannelType.WAN),
      blockedApplicationProtocols = Set(ApplicationProtocolType.FTP)
    )
    val edge = Edge(dummySource, dummyTarget, lanChannel, Some(httpProtocol))
    val result = FirewallPolicy.isBlocked(edge, policy)

    result shouldBe false
  }

  test("An edge with a blocked channel type is blocked regardless of protocol") {
    val policy = FirewallPolicy(
      blockedChannels = Set(ChannelType.WAN),
      blockedApplicationProtocols = Set.empty
    )
    val edge = Edge(dummySource, dummyTarget, wanChannel, Some(httpProtocol))
    val result = FirewallPolicy.isBlocked(edge, policy)

    result shouldBe true
  }

  test("An edge on LAN running a blocked application protocol is blocked") {
    val policy = FirewallPolicy(
      blockedChannels = Set.empty,
      blockedApplicationProtocols = Set(ApplicationProtocolType.FTP)
    )
    val edge = Edge(dummySource, dummyTarget, lanChannel, Some(ftpProtocol))
    val result = FirewallPolicy.isBlocked(edge, policy)

    result shouldBe true
  }

  test(
    "A VPN edge running a blocked protocol is NOT blocked because VPN hides application payload"
  ) {
    val policy = FirewallPolicy(
      blockedChannels = Set.empty,
      blockedApplicationProtocols = Set(ApplicationProtocolType.FTP)
    )
    val edge = Edge(dummySource, dummyTarget, vpnChannel, Some(ftpProtocol))
    // The firewall cannot inspect inside the VPN, so FTP is ignored and traffic passes
    val result = FirewallPolicy.isBlocked(edge, policy)

    result shouldBe false
  }

  test(
    "A VPN edge is blocked only if the VPN channel itself is explicitly included in blockedChannels"
  ) {
    val policy = FirewallPolicy(
      blockedChannels = Set(ChannelType.VPN),
      blockedApplicationProtocols = Set.empty
    )
    val edge = Edge(dummySource, dummyTarget, vpnChannel, Some(ftpProtocol))
    assert(FirewallPolicy.isBlocked(edge, policy))
    val result = FirewallPolicy.isBlocked(edge, policy)

    result shouldBe true
  }
