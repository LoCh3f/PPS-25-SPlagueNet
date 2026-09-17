package it.unibo.splague.persistence

import io.circe.derivation.Configuration
import io.circe.{Codec as CirceCodec, Decoder as CirceDecoder, Encoder as CirceEncoder}
import io.circe.syntax.*
import io.circe.parser.decode
import io.circe.generic.semiauto.*
import it.unibo.splague.model.node.NodeState
import it.unibo.splague.persistence.{Encoder, FileFormat}

object JsonCodecs:

  // Circe config
  given Configuration = Configuration.default

  // Custom encoders for sealed traits, enums
  given Encoder[NodeState, FileFormat.Json] with
    override def encode(a: NodeState): String = s"\"$a\""

  // Generic encoder for json objects
  given [A](using cEncoder: CirceEncoder[A]): Encoder[A, FileFormat.Json] with
    override def encode(a: A): String = a.asJson.noSpaces
