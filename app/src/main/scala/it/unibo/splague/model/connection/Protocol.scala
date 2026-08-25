package it.unibo.splague.model.connection

import it.unibo.splague.model.Probability

object Protocol:

  enum TransportProtocolType:
    case TCP, UDP

  enum ApplicationProtocolType:
    case HTTP, HTTPS, FTP, SSH, IMAP, Telnet

  trait TransportProtocol:
    def kind: TransportProtocolType
    def reliability: Either[String, Probability]

  trait ApplicationProtocol:
    def kind: ApplicationProtocolType
    def underlying: TransportProtocol

  case object TcpTransport extends TransportProtocol:
    val kind: TransportProtocolType = TransportProtocolType.TCP
    val reliability: Either[String, Probability] = Probability.apply(0.99)

  case object UdpTransport extends TransportProtocol:
    val kind: TransportProtocolType = TransportProtocolType.UDP
    val reliability: Either[String, Probability] = Probability.apply(0.90)
