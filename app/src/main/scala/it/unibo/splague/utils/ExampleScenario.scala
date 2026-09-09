package it.unibo.splague.utils

import it.unibo.splague.model.Probability
import it.unibo.splague.model.connection.Connection.{Channel, ChannelType, Edge}
import it.unibo.splague.model.malware.{
  Malware,
  MalwareKind,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.NodeState.Healthy
import it.unibo.splague.model.node.NodeType.{IoTDevice, Router, Server, Workstation}
import it.unibo.splague.model.node.NodeId
import it.unibo.splague.model.node.{Node, Topology}
import it.unibo.splague.simulation.Scenario

object ExampleScenario:
  def complexScenario(): Either[String, Scenario] =
    val ids: Vector[it.unibo.splague.model.node.NodeId.NodeId] = Vector(
      NodeId.of("core-router").toOption.get,
      NodeId.of("gateway").toOption.get,
      NodeId.of("mail-server").toOption.get,
      NodeId.of("db-server").toOption.get,
      NodeId.of("finance-ws").toOption.get,
      NodeId.of("hr-ws").toOption.get,
      NodeId.of("sales-ws").toOption.get,
      NodeId.of("vpn-gateway").toOption.get,
      NodeId.of("iot-camera").toOption.get,
      NodeId.of("iot-sensor").toOption.get,
      NodeId.of("mobile-analyst").toOption.get,
      NodeId.of("remote-admin").toOption.get
    )

    val nodes: Map[it.unibo.splague.model.node.NodeId.NodeId, Node] = Map(
      ids(0) -> Node(
        ids(0),
        Router,
        patchLevel = 0.8,
        defenseLevel = 0.9,
        state = Healthy,
        workload = 0.7,
        vectors = Set()
      ),
      ids(1) -> Node(
        ids(1),
        Server,
        patchLevel = 0.6,
        defenseLevel = 0.85,
        state = Healthy,
        workload = 0.8,
        vectors = Set()
      ),
      ids(2) -> Node(
        ids(2),
        Server,
        patchLevel = 0.7,
        defenseLevel = 0.9,
        state = Healthy,
        workload = 0.75,
        vectors = Set()
      ),
      ids(3) -> Node(
        ids(3),
        Server,
        patchLevel = 0.75,
        defenseLevel = 0.88,
        state = Healthy,
        workload = 0.82,
        vectors = Set()
      ),
      ids(4) -> Node(
        ids(4),
        Workstation,
        patchLevel = 0.55,
        defenseLevel = 0.68,
        state = Healthy,
        workload = 0.5,
        vectors = Set()
      ),
      ids(5) -> Node(
        ids(5),
        Workstation,
        patchLevel = 0.6,
        defenseLevel = 0.7,
        state = Healthy,
        workload = 0.52,
        vectors = Set()
      ),
      ids(6) -> Node(
        ids(6),
        Workstation,
        patchLevel = 0.58,
        defenseLevel = 0.72,
        state = Healthy,
        workload = 0.48,
        vectors = Set()
      ),
      ids(7) -> Node(
        ids(7),
        Router,
        patchLevel = 0.68,
        defenseLevel = 0.8,
        state = Healthy,
        workload = 0.65,
        vectors = Set()
      ),
      ids(8) -> Node(
        ids(8),
        IoTDevice,
        patchLevel = 0.2,
        defenseLevel = 0.3,
        state = Healthy,
        workload = 0.25,
        vectors = Set()
      ),
      ids(9) -> Node(
        ids(9),
        IoTDevice,
        patchLevel = 0.25,
        defenseLevel = 0.35,
        state = Healthy,
        workload = 0.3,
        vectors = Set()
      ),
      ids(10) -> Node(
        ids(10),
        Workstation,
        patchLevel = 0.65,
        defenseLevel = 0.78,
        state = Healthy,
        workload = 0.55,
        vectors = Set()
      ),
      ids(11) -> Node(
        ids(11),
        Workstation,
        patchLevel = 0.63,
        defenseLevel = 0.76,
        state = Healthy,
        workload = 0.57,
        vectors = Set()
      )
    )

    val edges: Set[Edge] = Set(
      Edge(
        nodes(ids(0)),
        nodes(ids(1)),
        Channel(
          ChannelType.LAN,
          bandwidth = 1000.0,
          latency = 2.0,
          jitter = 0.5,
          packetLoss = Probability.clamped(0.01)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(0)),
        nodes(ids(7)),
        Channel(
          ChannelType.VPN,
          bandwidth = 800.0,
          latency = 12.0,
          jitter = 1.2,
          packetLoss = Probability.clamped(0.03)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(1)),
        nodes(ids(2)),
        Channel(
          ChannelType.LAN,
          bandwidth = 1200.0,
          latency = 1.5,
          jitter = 0.4,
          packetLoss = Probability.clamped(0.02)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(1)),
        nodes(ids(3)),
        Channel(
          ChannelType.LAN,
          bandwidth = 1100.0,
          latency = 2.0,
          jitter = 0.6,
          packetLoss = Probability.clamped(0.02)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(1)),
        nodes(ids(4)),
        Channel(
          ChannelType.WAN,
          bandwidth = 500.0,
          latency = 15.0,
          jitter = 2.0,
          packetLoss = Probability.clamped(0.05)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(1)),
        nodes(ids(5)),
        Channel(
          ChannelType.WAN,
          bandwidth = 500.0,
          latency = 16.0,
          jitter = 2.2,
          packetLoss = Probability.clamped(0.05)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(1)),
        nodes(ids(6)),
        Channel(
          ChannelType.WAN,
          bandwidth = 450.0,
          latency = 18.0,
          jitter = 2.4,
          packetLoss = Probability.clamped(0.06)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(7)),
        nodes(ids(8)),
        Channel(
          ChannelType.WAN,
          bandwidth = 200.0,
          latency = 20.0,
          jitter = 3.0,
          packetLoss = Probability.clamped(0.08)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(7)),
        nodes(ids(9)),
        Channel(
          ChannelType.WAN,
          bandwidth = 220.0,
          latency = 22.0,
          jitter = 3.3,
          packetLoss = Probability.clamped(0.09)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(7)),
        nodes(ids(10)),
        Channel(
          ChannelType.WAN,
          bandwidth = 400.0,
          latency = 17.0,
          jitter = 2.1,
          packetLoss = Probability.clamped(0.04)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(7)),
        nodes(ids(11)),
        Channel(
          ChannelType.WAN,
          bandwidth = 420.0,
          latency = 19.0,
          jitter = 2.3,
          packetLoss = Probability.clamped(0.04)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(4)),
        nodes(ids(5)),
        Channel(
          ChannelType.LAN,
          bandwidth = 300.0,
          latency = 3.0,
          jitter = 0.8,
          packetLoss = Probability.clamped(0.02)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(5)),
        nodes(ids(6)),
        Channel(
          ChannelType.LAN,
          bandwidth = 320.0,
          latency = 3.2,
          jitter = 0.9,
          packetLoss = Probability.clamped(0.02)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(10)),
        nodes(ids(11)),
        Channel(
          ChannelType.LAN,
          bandwidth = 360.0,
          latency = 2.8,
          jitter = 0.7,
          packetLoss = Probability.clamped(0.02)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(2)),
        nodes(ids(4)),
        Channel(
          ChannelType.WAN,
          bandwidth = 600.0,
          latency = 14.0,
          jitter = 1.8,
          packetLoss = Probability.clamped(0.04)
        ),
        protocol = None
      ),
      Edge(
        nodes(ids(3)),
        nodes(ids(6)),
        Channel(
          ChannelType.WAN,
          bandwidth = 580.0,
          latency = 13.0,
          jitter = 1.7,
          packetLoss = Probability.clamped(0.04)
        ),
        protocol = None
      )
    )

    val topologyNodes: Map[String, Node] =
      nodes.iterator.map(entry => entry._1.value -> entry._2).toMap
    val topology = Topology(topologyNodes, edges)

    for
      malware <- Malware(
        name = "SimpleDropper",
        kind = MalwareKind.Worm,
        traits = MalwareTraits(
          infectivity = Probability.clamped(0.82),
          stealth = Probability.clamped(0.68),
          payloadSeverity = PayloadSeverityLevel.Medium,
          persistence = Probability.clamped(0.61),
          footprint = Probability.clamped(0.42)
        ),
        vectors = Set(PropagationVector.NetworkExploit)
      )
      scenario <- Scenario(
        name = "Complex enterprise mesh",
        topology = topology,
        virus = malware,
        startingNode = nodes(ids(0)),
        tick = 0,
        seed = 42,
        maxIterations = 60
      )
    yield scenario
