package it.unibo.splague.model.connection

import it.unibo.splague.model.Probability
import it.unibo.splague.model.*
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ConnectionSuite extends AnyFunSuite:

  private val packetLoss = Probability.apply(0.1).getOrElse(fail("packet loss should be valid"))

  private val sourceNode = Node(
    NodeId.of("node-01").getOrElse(fail("source NodeId should be valid")),
    NodeType.Router,
    0.1,
    0.5,
    NodeState.Healthy,
    0.2
  )

  private val targetNode = Node(
    NodeId.of("node-02").getOrElse(fail("target NodeId should be valid")),
    NodeType.Workstation,
    0.2,
    0.6,
    NodeState.Infected,
    0.9
  )

  private val httpProtocol: Protocol.ApplicationProtocol = new Protocol.ApplicationProtocol:
    override def kind: Protocol.ApplicationProtocolType = Protocol.ApplicationProtocolType.HTTP
    override def underlying: Protocol.TransportProtocol = Protocol.TcpTransport

  test("channel types should include the supported network categories"):
    Connection.ChannelType.values.toSet shouldBe Set(
      Connection.ChannelType.LAN,
      Connection.ChannelType.WAN,
      Connection.ChannelType.VPN
    )

  test("channel should expose all configured network metrics"):
    val channel = Connection.Channel(
      channelType = Connection.ChannelType.WAN,
      bandwidth = 1000.0,
      latency = 25.0,
      jitter = 5.0,
      packetLoss = packetLoss
    )

    channel.channelType shouldBe Connection.ChannelType.WAN
    channel.bandwidth shouldBe 1000.0
    channel.latency shouldBe 25.0
    channel.jitter shouldBe 5.0
    channel.packetLoss shouldBe packetLoss

  test("edge should preserve source, target, channel, and optional protocol"):
    val channel = Connection.Channel(
      channelType = Connection.ChannelType.LAN,
      bandwidth = 500.0,
      latency = 10.0,
      jitter = 2.0,
      packetLoss = packetLoss
    )

    val edge = Connection.Edge(sourceNode, targetNode, channel, Some(httpProtocol))

    edge.source shouldBe sourceNode
    edge.target shouldBe targetNode
    edge.channel shouldBe channel
    edge.protocol shouldBe Some(httpProtocol)
    edge.protocol.get.kind shouldBe Protocol.ApplicationProtocolType.HTTP
    edge.protocol.get.underlying shouldBe Protocol.TcpTransport

  test("edge should allow a missing protocol"):
    val channel = Connection.Channel(
      channelType = Connection.ChannelType.VPN,
      bandwidth = 250.0,
      latency = 15.0,
      jitter = 1.0,
      packetLoss = packetLoss
    )

    val edge = Connection.Edge(sourceNode, targetNode, channel, None)
    edge.protocol shouldBe None
