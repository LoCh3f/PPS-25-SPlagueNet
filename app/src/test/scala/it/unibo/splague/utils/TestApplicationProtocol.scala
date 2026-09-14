package it.unibo.splague.utils

import it.unibo.splague.model.connection.Protocol.{
  ApplicationProtocol,
  ApplicationProtocolType,
  TcpTransport,
  TransportProtocol
}

case class TestApplicationProtocol(
    kind: ApplicationProtocolType,
    underlying: TransportProtocol = TcpTransport
) extends ApplicationProtocol
