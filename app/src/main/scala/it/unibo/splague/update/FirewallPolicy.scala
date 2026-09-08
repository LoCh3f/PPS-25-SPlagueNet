package it.unibo.splague.update

import it.unibo.splague.model.connection.Connection
import it.unibo.splague.model.connection.Connection.{ChannelType, Edge}
import it.unibo.splague.model.connection.Protocol.{ApplicationProtocol, ApplicationProtocolType}

case class FirewallPolicy(
    blockedChannels: Set[ChannelType] = Set.empty,
    blockedApplicationProtocols: Set[ApplicationProtocolType] = Set.empty
):
  /** Merges two firewall policies by creating a new one with the union of both blockedChannels and
    * blockedApplicationProtocols
    * @param other
    *   FirewallPolicy
    * @return
    *   New merged FirewallPolicy
    */
  def merge(other: FirewallPolicy): FirewallPolicy = FirewallPolicy(
    blockedChannels ++ other.blockedChannels,
    blockedApplicationProtocols ++ other.blockedApplicationProtocols
  )

object FirewallPolicy:

  /** The WAN channel type and the Telnet/FTP application protocols are the most vulnerable
    * (passwords in clear). Thus, we set them as defaults to defend against.
    */
  val defaultPolicy = FirewallPolicy(
    blockedChannels = Set(ChannelType.WAN),
    blockedApplicationProtocols = Set(ApplicationProtocolType.Telnet, ApplicationProtocolType.FTP)
  )

  /** Given an edge and a FirewallPolicy, it controls weather that edge is blocked by a FireWall
    * countermeasure.
    * @param edge
    *   between two nodes
    * @param policy
    *   of the Firewall
    * @return
    */
  def isBlocked(edge: Edge, policy: FirewallPolicy) =
    if policy.blockedChannels.contains(edge.channel.channelType) then true
    else
      edge.channel.channelType match
        case Connection.ChannelType.VPN =>
          false // inside a VPN the application protocol can't be inspected
        case _ => edge.protocol.exists(p => policy.blockedApplicationProtocols.contains(p.kind))
