package it.unibo.splague.model.connection

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ProtocolSuite extends AnyFunSuite:

  test("transport protocol types should match exactly the supported protocols"):
    Protocol.TransportProtocolType.values.toSet shouldBe Set(
      Protocol.TransportProtocolType.TCP,
      Protocol.TransportProtocolType.UDP
    )

  test("application protocol types should match exactly the supported protocols"):
    Protocol.ApplicationProtocolType.values.toSet shouldBe Set(
      Protocol.ApplicationProtocolType.HTTP,
      Protocol.ApplicationProtocolType.HTTPS,
      Protocol.ApplicationProtocolType.FTP,
      Protocol.ApplicationProtocolType.SSH,
      Protocol.ApplicationProtocolType.IMAP,
      Protocol.ApplicationProtocolType.Telnet
    )

  test("TCP transport should expose the expected kind and valid reliability"):
    Protocol.TcpTransport.kind shouldBe Protocol.TransportProtocolType.TCP
    Protocol.TcpTransport.reliability.map(_.value) shouldBe Right(0.99)

  test("UDP transport should expose the expected kind and valid reliability"):
    Protocol.UdpTransport.kind shouldBe Protocol.TransportProtocolType.UDP
    Protocol.UdpTransport.reliability.map(_.value) shouldBe Right(0.90)

  test("transport reliability should stay within the valid probability range"):
    Protocol.TcpTransport.reliability.map(_.value).forall(v => v >= 0.0 && v <= 1.0) shouldBe true
    Protocol.UdpTransport.reliability.map(_.value).forall(v => v >= 0.0 && v <= 1.0) shouldBe true
