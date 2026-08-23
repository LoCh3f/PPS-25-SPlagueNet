package it.unibo.splague.model

import it.unibo.splague.model.Probability.Probability

object Connection:

  enum TransportProtocolType:
    case TCP, UDP, ICMP

  enum ApplicationProtocolType:
    case HTTP, HTTPS, FTP, SSH, IMAP, Telnet

  trait TransportProtocol:
    def kind: TransportProtocolType
    def reliability: Probability

  trait ApplicationProtocol:
    def kind: ApplicationProtocolType
    def underlying: TransportProtocol

  trait Channel:
    def bandwidth: Double
    def latency: Double
    def jitter: Double
    def packetLoss: Probability
    def reliability: Probability

  case class Edge(source: Node, target: Node, channel: Channel)
