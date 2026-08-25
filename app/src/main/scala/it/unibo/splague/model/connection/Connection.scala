package it.unibo.splague.model.connection

import it.unibo.splague.model.Probability.Probability
import it.unibo.splague.model.connection.Protocol.ApplicationProtocol
import it.unibo.splague.model.node.Node

object Connection:

  enum ChannelType:
    case LAN, WAN, VPN

  case class Channel(
      channelType: ChannelType,
      bandwidth: Double,
      latency: Double,
      jitter: Double,
      packetLoss: Probability
  )

  case class Edge(
      source: Node,
      target: Node,
      channel: Channel,
      protocol: Option[ApplicationProtocol]
  )
