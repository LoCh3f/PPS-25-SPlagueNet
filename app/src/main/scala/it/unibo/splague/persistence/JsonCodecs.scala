package it.unibo.splague.persistence

import io.circe.derivation.Configuration
import io.circe.{Codec as CirceCodec, Decoder as CirceDecoder, Encoder as CirceEncoder}
import io.circe.syntax.*
import io.circe.parser.decode
import io.circe.generic.semiauto.*
import it.unibo.splague.model.node.NodeState
import it.unibo.splague.model.node.NodeState.{Destroyed, Healthy, Immune, Infected, Quarantined}
import it.unibo.splague.persistence.{Encoder, FileFormat}

object JsonCodecs:

  // Circe config
  given Configuration = Configuration.default

  // Custom encoders for sealed traits, enums
  given Encoder[NodeState, FileFormat.Json] with
    override def encode(a: NodeState): String = s"\"$a\""

  // Custom decoder for sealed traits, enums
  given Decoder[NodeState, FileFormat.Json] with
    override def decode(raw: String): Either[PersistenceError, NodeState] =
      raw.trim.stripPrefix("\"").stripSuffix("\"") match
        case "Healthy"     => Right(Healthy)
        case "Infected"    => Right(Infected)
        case "Quarantined" => Right(Quarantined)
        case "Immune"      => Right(Immune)
        case "Destroyed"   => Right(Destroyed)
        case other         => Left(PersistenceError.Parsing(s"Unknown NodeState: $other"))

  // Generic encoder for json objects
  given [A](using cEncoder: CirceEncoder[A]): Encoder[A, FileFormat.Json] with
    override def encode(a: A): String = a.asJson.noSpaces

  // Generic decoder
  given [A](using cDecoder: CirceDecoder[A]): Decoder[A, FileFormat.Json] with
    override def decode(raw: String): Either[PersistenceError, A] =
      io.circe.parser.decode[A](raw) match
        case Right(value) => Right(value)
        case Left(error)  => Left(PersistenceError.Parsing(error.getMessage))
