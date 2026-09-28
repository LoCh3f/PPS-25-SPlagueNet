package it.unibo.splague.persistence.codecs.json

import io.circe.derivation.Configuration
import io.circe.generic.auto.{deriveDecoder, deriveEncoder}
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.{Json, Codec as CirceCodec, Decoder as CirceDecoder, Encoder as CirceEncoder}
import it.unibo.splague.model.connection.Connection.{Channel, ChannelType, Edge}
import it.unibo.splague.model.connection.Protocol.*
import it.unibo.splague.model.countermeasures.{CountermeasureConfig, Countermeasures}
import it.unibo.splague.model.malware.{Malware, MalwareTraits}
import it.unibo.splague.model.node.*
import it.unibo.splague.model.node.NodeId.NodeId
import it.unibo.splague.model.report.{Milestones, TickSummary}
import it.unibo.splague.model.{Awareness, Probability, Scenario}
import it.unibo.splague.persistence.*
import it.unibo.splague.update.IsolationCriteria
import it.unibo.splague.update.simulation.report.ScenarioReport

case class TestApplicationProtocol(
    kind: ApplicationProtocolType,
    underlying: TransportProtocol = TcpTransport
) extends ApplicationProtocol

object CodecCatalog:

  // --- Circe Configuration ---
  given Configuration = Configuration.default

  // ---  ATOMIC & LEAF TYPES (Value Classes, Opaque Types, Enums) ---------

  // NodeId
  given nodeIdCirceEncoder: CirceEncoder[NodeId] =
    CirceEncoder.encodeString.contramap(_.value)

  given nodeIdCirceDecoder: CirceDecoder[NodeId] =
    CirceDecoder.decodeString.emap { raw =>
      NodeId.of(raw) match
        case Right(id) => Right(id)
        case Left(err) => Left(err)
    }

  // Awareness
  given awarenessCirceEncoder: CirceEncoder[Awareness] =
    CirceEncoder.encodeDouble.contramap(_.value)

  given awarenessCirceDecoder: CirceDecoder[Awareness] =
    CirceDecoder.decodeDouble.emap { d =>
      Awareness(d) match
        case Right(a)  => Right(a)
        case Left(err) => Left(err)
    }

  // Probability
  given probabilityCirceEncoder: CirceEncoder[Probability] =
    CirceEncoder.encodeDouble.contramap(_.value)

  given probabilityCirceDecoder: CirceDecoder[Probability] =
    CirceDecoder.decodeDouble.emap { d =>
      Probability(d) match
        case Right(p)  => Right(p)
        case Left(err) => Left(err)
    }

  // NodeState
  given nodeStateCirceEncoder: CirceEncoder[NodeState] =
    CirceEncoder.encodeString.contramap(_.toString)

  given nodeStateCirceDecoder: CirceDecoder[NodeState] =
    CirceDecoder.decodeString.emap {
      case "Healthy"     => Right(NodeState.Healthy)
      case "Infected"    => Right(NodeState.Infected)
      case "Quarantined" => Right(NodeState.Quarantined)
      case "Immune"      => Right(NodeState.Immune)
      case "Destroyed"   => Right(NodeState.Destroyed)
      case other         => Left(s"Unknown NodeState: $other")
    }

  // NodeType
  given nodeTypeCirceEncoder: CirceEncoder[NodeType] =
    CirceEncoder.encodeString.contramap(_.toString)

  given nodeTypeCirceDecoder: CirceDecoder[NodeType] =
    CirceDecoder.decodeString.emap {
      case "Workstation"  => Right(NodeType.Workstation)
      case "Server"       => Right(NodeType.Server)
      case "Router"       => Right(NodeType.Router)
      case "IoTDevice"    => Right(NodeType.IoTDevice)
      case "MobileDevice" => Right(NodeType.MobileDevice)
      case other          => Left(s"Unknown NodeType: $other")
    }

  // Countermeasures
  given countermeasuresCirceEncoder: CirceEncoder[Countermeasures] =
    CirceEncoder.encodeString.contramap(_.toString)

  given countermeasuresCirceDecoder: CirceDecoder[Countermeasures] =
    CirceDecoder.decodeString.emap {
      case "DefenseBoost" => Right(Countermeasures.DefenseBoost)
      case "Firewall"     => Right(Countermeasures.Firewall)
      case "Isolation"    => Right(Countermeasures.Isolation)
      case "Patch"        => Right(Countermeasures.Patch)
      case other          => Left(s"Unknown Countermeasures: $other")
    }

  // ChannelType
  given channelTypeCirceEncoder: CirceEncoder[ChannelType] =
    CirceEncoder.encodeString.contramap(_.toString)

  given channelTypeCirceDecoder: CirceDecoder[ChannelType] =
    CirceDecoder.decodeString.emap {
      case "LAN" => Right(ChannelType.LAN)
      case "WAN" => Right(ChannelType.WAN)
      case "VPN" => Right(ChannelType.VPN)
      case other => Left(s"Unknown ChannelType: $other")
    }

  // TransportProtocolType
  given transportProtocolTypeEncoder: CirceEncoder[TransportProtocolType] =
    CirceEncoder.encodeString.contramap(_.toString)

  given transportProtocolTypeDecoder: CirceDecoder[TransportProtocolType] =
    CirceDecoder.decodeString.emap {
      case "TCP" => Right(TransportProtocolType.TCP)
      case "UDP" => Right(TransportProtocolType.UDP)
      case other => Left(s"Unknown TransportProtocolType: $other")
    }

  // ApplicationProtocolType
  given applicationProtocolTypeEncoder: CirceEncoder[ApplicationProtocolType] =
    CirceEncoder.encodeString.contramap(_.toString)

  given applicationProtocolTypeDecoder: CirceDecoder[ApplicationProtocolType] =
    CirceDecoder.decodeString.emap {
      case "HTTP"   => Right(ApplicationProtocolType.HTTP)
      case "HTTPS"  => Right(ApplicationProtocolType.HTTPS)
      case "FTP"    => Right(ApplicationProtocolType.FTP)
      case "SSH"    => Right(ApplicationProtocolType.SSH)
      case "IMAP"   => Right(ApplicationProtocolType.IMAP)
      case "Telnet" => Right(ApplicationProtocolType.Telnet)
      case other    => Left(s"Unknown ApplicationProtocolType: $other")
    }

  // --- PROTOCOLS, STUBS & COMPLEX COLLECTIONS ---------------------------

  // TransportProtocol
  given transportProtocolEncoder: CirceEncoder[TransportProtocol] =
    CirceEncoder.encodeString.contramap {
      case TcpTransport => "TCP"
      case UdpTransport => "UDP"
    }

  given transportProtocolDecoder: CirceDecoder[TransportProtocol] =
    CirceDecoder.decodeString.emap {
      case "TCP" => Right(TcpTransport)
      case "UDP" => Right(UdpTransport)
      case other => Left(s"Unknown TransportProtocol: $other")
    }

  // ApplicationProtocol
  given applicationProtocolEncoder: CirceEncoder[ApplicationProtocol] =
    CirceEncoder.instance { ap =>
      Json.obj(
        "kind" -> ap.kind.asJson,
        "underlying" -> ap.underlying.asJson
      )
    }

  given applicationProtocolDecoder: CirceDecoder[ApplicationProtocol] =
    CirceDecoder.instance { cursor =>
      for
        kind <- cursor.downField("kind").as[ApplicationProtocolType]
        underlying <- cursor.downField("underlying").as[TransportProtocol]
      yield TestApplicationProtocol(kind, underlying): ApplicationProtocol
    }

  // IsolationCriteria (Stub workaround for functions)
  given isolationCriteriaCirceCodec: CirceCodec[IsolationCriteria] =
    CirceCodec.from(
      io.circe.Decoder.const(IsolationCriteria.all),
      io.circe.Encoder.instance(_ => Json.fromString(""))
    )

  // Complex Maps (serialized as lists of tuples to support non-string keys)
  given mapNodeDoubleEncoder: CirceEncoder[Map[Node, Double]] =
    CirceEncoder.encodeList[(Node, Double)].contramap(_.toList)

  given mapNodeDoubleDecoder: CirceDecoder[Map[Node, Double]] =
    CirceDecoder.decodeList[(Node, Double)].map(_.toMap)

  given mapDoubleCountermeasuresEncoder: CirceEncoder[Map[Double, Countermeasures]] =
    CirceEncoder.encodeList[(Double, Countermeasures)].contramap(_.toList)

  given mapDoubleCountermeasuresDecoder: CirceDecoder[Map[Double, Countermeasures]] =
    CirceDecoder.decodeList[(Double, Countermeasures)].map(_.toMap)

  // Report
  given mapCountermeasuresIntEncoder: CirceEncoder[Map[Countermeasures, Int]] =
    CirceEncoder.encodeList[(Countermeasures, Int)].contramap(_.toList)

  given mapCountermeasuresIntDecoder: CirceDecoder[Map[Countermeasures, Int]] =
    CirceDecoder.decodeList[(Countermeasures, Int)].map(_.toMap)

  given tickSummaryCirceCodec: CirceCodec[TickSummary] = deriveCodec[TickSummary]

  given milestonesCirceCodec: CirceCodec[Milestones] = deriveCodec[Milestones]

  given scenarioReportCirceCodec: CirceCodec[ScenarioReport] = deriveCodec[ScenarioReport]

  // --- INTERMEDIATE MODEL STRUCTURES ------------------------------------

  given channelCirceCodec: CirceCodec[Channel] = deriveCodec[Channel]

  given nodeCirceCodec: CirceCodec[Node] = deriveCodec[Node]

  given edgeCirceCodec: CirceCodec[Edge] = deriveCodec[Edge]

  // --- AGGREGATE DOMAIN ENTITIES & ROOT (Scenario) ----------------------

  given countermeasureConfigCirceCodec: CirceCodec[CountermeasureConfig] =
    deriveCodec[CountermeasureConfig]

  given topologyCirceCodec: CirceCodec[Topology] =
    deriveCodec[Topology]

  given malwareTraitsCirceCodec: CirceCodec[MalwareTraits] =
    deriveCodec[MalwareTraits]

  given malwareCirceCodec: CirceCodec[Malware] =
    deriveCodec[Malware]

  given scenarioCirceCodec: CirceCodec[Scenario] =
    deriveCodec[Scenario]
