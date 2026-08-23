package it.unibo.splague.model.connection

import it.unibo.splague.model.Node
import it.unibo.splague.model.Probability.Probability
import it.unibo.splague.model.connection.Protocol.ApplicationProtocol

object Connection:

  enum ChannelType:
    case LAN, WAN, VPN

  trait Channel:
    def kind: ChannelType
    def bandwidth: Double
    def latency: Double
    def jitter: Double
    def packetLoss: Probability
    def reliability: Probability

  case class Edge(source: Node, target: Node, channel: Channel, protocol: ApplicationProtocol)
