package it.unibo.splague.utils

import it.unibo.splague.dsl.*
import it.unibo.splague.model.Scenario
import it.unibo.splague.model.connection.Connection.ChannelType.{LAN, VPN, WAN}
import it.unibo.splague.model.malware.MalwareKind.Worm
import it.unibo.splague.model.malware.PayloadSeverityLevel.Medium
import it.unibo.splague.model.malware.PropagationVector.NetworkExploit
import it.unibo.splague.model.node.NodeType.{IoTDevice, Router, Server, Workstation}

// $COVERAGE-OFF$
object ExampleScenario:
  def complexScenario(): Either[String, Scenario] =
    val result: ValidationResult[Scenario] =
      scenario("Complex enterprise mesh"):
        seed(42)
        maxIterations(60)

        network:
          node("core-router", Router, patchLevel = 0.8, defenseLevel = 0.9, workload = 0.7)
          node("gateway", Server, patchLevel = 0.6, defenseLevel = 0.85, workload = 0.8)
          node("mail-server", Server, patchLevel = 0.7, defenseLevel = 0.9, workload = 0.75)
          node("db-server", Server, patchLevel = 0.75, defenseLevel = 0.88, workload = 0.82)
          node("finance-ws", Workstation, patchLevel = 0.55, defenseLevel = 0.68, workload = 0.5)
          node("hr-ws", Workstation, patchLevel = 0.6, defenseLevel = 0.7, workload = 0.52)
          node("sales-ws", Workstation, patchLevel = 0.58, defenseLevel = 0.72, workload = 0.48)
          node("vpn-gateway", Router, patchLevel = 0.68, defenseLevel = 0.8, workload = 0.65)
          node("iot-camera", IoTDevice, patchLevel = 0.2, defenseLevel = 0.3, workload = 0.25)
          node("iot-sensor", IoTDevice, patchLevel = 0.25, defenseLevel = 0.35, workload = 0.3)
          node(
            "mobile-analyst",
            Workstation,
            patchLevel = 0.65,
            defenseLevel = 0.78,
            workload = 0.55
          )
          node("remote-admin", Workstation, patchLevel = 0.63, defenseLevel = 0.76, workload = 0.57)

          "core-router" <-> "gateway" via (
            LAN,
            ChannelOverrides(Some(1000.0), Some(2.0), Some(0.5), Some(0.01))
          )
          "core-router" <-> "vpn-gateway" via (
            VPN,
            ChannelOverrides(Some(800.0), Some(12.0), Some(1.2), Some(0.03))
          )
          "gateway" <-> "mail-server" via (
            LAN,
            ChannelOverrides(Some(1200.0), Some(1.5), Some(0.4), Some(0.02))
          )
          "gateway" <-> "db-server" via (
            LAN,
            ChannelOverrides(Some(1100.0), Some(2.0), Some(0.6), Some(0.02))
          )
          "gateway" <-> "finance-ws" via (
            WAN,
            ChannelOverrides(Some(500.0), Some(15.0), Some(2.0), Some(0.05))
          )
          "gateway" <-> "hr-ws" via (
            WAN,
            ChannelOverrides(Some(500.0), Some(16.0), Some(2.2), Some(0.05))
          )
          "gateway" <-> "sales-ws" via (
            WAN,
            ChannelOverrides(Some(450.0), Some(18.0), Some(2.4), Some(0.06))
          )
          "vpn-gateway" <-> "iot-camera" via (
            WAN,
            ChannelOverrides(Some(200.0), Some(20.0), Some(3.0), Some(0.08))
          )
          "vpn-gateway" <-> "iot-sensor" via (
            WAN,
            ChannelOverrides(Some(220.0), Some(22.0), Some(3.3), Some(0.09))
          )
          "vpn-gateway" <-> "mobile-analyst" via (
            WAN,
            ChannelOverrides(Some(400.0), Some(17.0), Some(2.1), Some(0.04))
          )
          "vpn-gateway" <-> "remote-admin" via (
            WAN,
            ChannelOverrides(Some(420.0), Some(19.0), Some(2.3), Some(0.04))
          )
          "finance-ws" <-> "hr-ws" via (
            LAN,
            ChannelOverrides(Some(300.0), Some(3.0), Some(0.8), Some(0.02))
          )
          "hr-ws" <-> "sales-ws" via (
            LAN,
            ChannelOverrides(Some(320.0), Some(3.2), Some(0.9), Some(0.02))
          )
          "mobile-analyst" <-> "remote-admin" via (
            LAN,
            ChannelOverrides(Some(360.0), Some(2.8), Some(0.7), Some(0.02))
          )
          "mail-server" <-> "finance-ws" via (
            WAN,
            ChannelOverrides(Some(600.0), Some(14.0), Some(1.8), Some(0.04))
          )
          "db-server" <-> "sales-ws" via (
            WAN,
            ChannelOverrides(Some(580.0), Some(13.0), Some(1.7), Some(0.04))
          )

        malware(
          "SimpleDropper",
          Worm,
          infectivity = 0.82,
          stealth = 0.68,
          severity = Medium,
          persistence = 0.61,
          footprint = 0.42,
          vectors = Set(NetworkExploit)
        )

        startingNode("core-router")

    result.left.map(_.mkString("; "))
// $COVERAGE-ON$
