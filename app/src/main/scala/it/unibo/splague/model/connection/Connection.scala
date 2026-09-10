package it.unibo.splague.model.connection

import it.unibo.splague.model.Probability
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

  object Channel:
    /** A channel with baseline parameters for the given type, sourced from
      * `SimulationConfig.Channels`.
      */
    def default(channelType: ChannelType): Channel =
      import it.unibo.splague.config.SimulationConfig.Channels.*
      channelType match
        case ChannelType.LAN =>
          Channel(
            ChannelType.LAN,
            LAN_BANDWIDTH,
            LAN_LATENCY,
            LAN_JITTER,
            Probability.clamped(LAN_PACKET_LOSS)
          )
        case ChannelType.WAN =>
          Channel(
            ChannelType.WAN,
            WAN_BANDWIDTH,
            WAN_LATENCY,
            WAN_JITTER,
            Probability.clamped(WAN_PACKET_LOSS)
          )
        case ChannelType.VPN =>
          Channel(
            ChannelType.VPN,
            VPN_BANDWIDTH,
            VPN_LATENCY,
            VPN_JITTER,
            Probability.clamped(VPN_PACKET_LOSS)
          )

    /** `default(channelType)` with any provided fields overridden. */
    def default(
        channelType: ChannelType,
        bandwidth: Option[Double],
        latency: Option[Double],
        jitter: Option[Double],
        packetLoss: Option[Double]
    ): Channel =
      val base = default(channelType)
      base.copy(
        bandwidth = bandwidth.getOrElse(base.bandwidth),
        latency = latency.getOrElse(base.latency),
        jitter = jitter.getOrElse(base.jitter),
        packetLoss = packetLoss.fold(base.packetLoss)(Probability.clamped)
      )

  case class Edge(
      source: Node,
      target: Node,
      channel: Channel,
      protocol: Option[ApplicationProtocol]
  )
