package it.unibo.splague.persistence

import io.circe.derivation.Configuration
import io.circe.{Codec as CirceCodec, Decoder as CirceDecoder, Encoder as CirceEncoder}
import io.circe.syntax.*
import io.circe.parser.decode
import io.circe.generic.semiauto.*
import io.circe.generic.auto.deriveDecoder
import io.circe.generic.auto.deriveEncoder
import it.unibo.splague.model.Awareness
import it.unibo.splague.model.node.NodeId.NodeId
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType}
import it.unibo.splague.model.node.NodeState.{Destroyed, Healthy, Immune, Infected, Quarantined}
import it.unibo.splague.persistence.{Encoder, FileFormat}

object JsonCodecs:

  // --- Circe Configuration ---
  given Configuration = Configuration.default

  // CIRCE ENCODERS/DECODERS

  // NodeId (with validation of NodeId.of)
  given nodeIdCirceEncoder: CirceEncoder[NodeId] =
    CirceEncoder.encodeString.contramap(_.value)

  given nodeIdCirceDecoder: CirceDecoder[NodeId] =
    CirceDecoder.decodeString.emap { raw =>
      NodeId.of(raw) match
        case Right(id) => Right(id)
        case Left(err) => Left(err)
    }

  // Awareness (with validation in range [0, 1])
  given awarenessCirceEncoder: CirceEncoder[Awareness] =
    CirceEncoder.encodeDouble.contramap(_.value)

  given awarenessCirceDecoder: CirceDecoder[Awareness] =
    CirceDecoder.decodeDouble.emap { d =>
      Awareness(d) match
        case Right(a)  => Right(a)
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

  // Map[Node, Double] (serialization with list of tuples)
  given mapNodeDoubleEncoder: CirceEncoder[Map[Node, Double]] =
    CirceEncoder.encodeList[(Node, Double)].contramap(_.toList)

  given mapNodeDoubleDecoder: CirceDecoder[Map[Node, Double]] =
    CirceDecoder.decodeList[(Node, Double)].map(_.toMap)

  // Generic json encoders/decoders that use CirceEncoders/Decoders
  given [A](using cEncoder: CirceEncoder[A]): Encoder[A, FileFormat.Json] with
    override def encode(a: A): String = cEncoder(a).noSpaces

  given [A](using cDecoder: CirceDecoder[A]): Decoder[A, FileFormat.Json] with
    override def decode(raw: String): Either[PersistenceError, A] =
      io.circe.parser.decode[A](raw) match
        case Right(value) => Right(value)
        case Left(error)  => Left(PersistenceError.Parsing(error.getMessage))
