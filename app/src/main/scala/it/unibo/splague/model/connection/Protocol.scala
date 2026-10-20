package it.unibo.splague.model.connection

import it.unibo.splague.model.Probability
import it.unibo.splague.utils.config.SimulationConfig.Protocols.{TCP_RELIABILITY, UDP_RELIABILITY}

object Protocol:

  enum TransportProtocolType:
    case TCP, UDP

  enum ApplicationProtocolType:
    case HTTP, HTTPS, FTP, SSH, IMAP, Telnet

  trait TransportProtocol:
    def kind: TransportProtocolType
    def reliability: Probability

  trait ApplicationProtocol:
    def kind: ApplicationProtocolType
    def underlying: TransportProtocol

  case object TcpTransport extends TransportProtocol:
    val kind: TransportProtocolType = TransportProtocolType.TCP
    val reliability: Probability = Probability.apply(TCP_RELIABILITY).toOption.get

  case object UdpTransport extends TransportProtocol:
    val kind: TransportProtocolType = TransportProtocolType.UDP
    val reliability: Probability = Probability.apply(UDP_RELIABILITY).toOption.get
