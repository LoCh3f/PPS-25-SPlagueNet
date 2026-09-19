package it.unibo.splague.persistence.codecs.json

import io.circe.{Decoder as CirceDecoder, Encoder as CirceEncoder}
import it.unibo.splague.persistence.*
import it.unibo.splague.persistence.codecs.{Decoder, Encoder}

/** Provides generic JSON-based implementations for the framework's persistence `Encoder` and
  * `Decoder` interfaces, delegating the actual serialization and parsing work to underlying Circe
  * typeclasses.
  */
object JsonCodec {

  /** Bridges the framework's generic [[Encoder]] for [[FileFormat.Json]] with an underlying Circe
    * `Encoder`.
    *
    * It automatically converts any domain entity `A` into its compact JSON string representation
    * (`noSpaces`) by summoning the corresponding Circe encoder `given`.
    *
    * @tparam A
    *   the type of the domain entity to encode
    * @param cEncoder
    *   the underlying Circe encoder for type `A`
    * @return
    *   an instance of [[Encoder]] specialized for [[FileFormat.Json]]
    */
  given [A](using cEncoder: CirceEncoder[A]): Encoder[A, FileFormat.Json.type] with
    override def encode(a: A): String = cEncoder(a).noSpaces

  given [A](using cDecoder: CirceDecoder[A]): Decoder[A, FileFormat.Json.type] with
    override def decode(raw: String): Either[PersistenceError, A] =
      io.circe.parser.decode[A](raw) match
        case Right(value) => Right(value)
        case Left(error)  => Left(PersistenceError.Parsing(error.getMessage))

}
