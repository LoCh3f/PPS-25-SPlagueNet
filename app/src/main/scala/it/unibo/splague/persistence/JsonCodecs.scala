package it.unibo.splague.persistence

import io.circe.derivation.Configuration
import io.circe.{Codec as CirceCodec, Decoder as CirceDecoder, Encoder as CirceEncoder}
import io.circe.syntax.*
import io.circe.parser.decode
import io.circe.generic.semiauto.*
import it.unibo.splague.model.Awareness
import it.unibo.splague.model.node.{NodeState, NodeType}
import it.unibo.splague.model.node.NodeState.{Destroyed, Healthy, Immune, Infected, Quarantined}
import it.unibo.splague.persistence.{Encoder, FileFormat}

object JsonCodecs:

  // Circe config
  given Configuration = Configuration.default

  // Custom encoders for atomic types
  given Encoder[NodeState, FileFormat.Json] with
    override def encode(a: NodeState): String = s"\"$a\""

  given Encoder[NodeType, FileFormat.Json] with
    override def encode(a: NodeType): String = s"\"$a\""

  given Encoder[Awareness, FileFormat.Json] with
    override def encode(a: Awareness): String = a.value.toString

  // Custom decoders for atomic types
  given Decoder[NodeState, FileFormat.Json] with
    override def decode(raw: String): Either[PersistenceError, NodeState] =
      raw.trim.stripPrefix("\"").stripSuffix("\"") match
        case "Healthy"     => Right(Healthy)
        case "Infected"    => Right(Infected)
        case "Quarantined" => Right(Quarantined)
        case "Immune"      => Right(Immune)
        case "Destroyed"   => Right(Destroyed)
        case other         => Left(PersistenceError.Parsing(s"Unknown NodeState: $other"))

  given Decoder[NodeType, FileFormat.Json] with
    override def decode(raw: String): Either[PersistenceError, NodeType] =
      raw.trim.stripPrefix("\"").stripSuffix("\"") match
        case "Workstation"  => Right(NodeType.Workstation)
        case "Server"       => Right(NodeType.Server)
        case "Router"       => Right(NodeType.Router)
        case "IoTDevice"    => Right(NodeType.IoTDevice)
        case "MobileDevice" => Right(NodeType.MobileDevice)
        case other          => Left(PersistenceError.Parsing(s"Unknown NodeType: $other"))

  given Decoder[Awareness, FileFormat.Json] with
    override def decode(raw: String): Either[PersistenceError, Awareness] =
      raw.trim.toDoubleOption match
        case Some(doubleVal) =>
          Awareness(doubleVal) match
            case Right(awareness) => Right(awareness)
            case Left(err)        => Left(PersistenceError.Parsing(err))
        case None =>
          Left(PersistenceError.Parsing(s"Invalid number format for Awareness: $raw"))

  // Generic encoder for json objects
  given [A](using cEncoder: CirceEncoder[A]): Encoder[A, FileFormat.Json] with
    override def encode(a: A): String = a.asJson.noSpaces

  // Generic decoder
  given [A](using cDecoder: CirceDecoder[A]): Decoder[A, FileFormat.Json] with
    override def decode(raw: String): Either[PersistenceError, A] =
      io.circe.parser.decode[A](raw) match
        case Right(value) => Right(value)
        case Left(error)  => Left(PersistenceError.Parsing(error.getMessage))
