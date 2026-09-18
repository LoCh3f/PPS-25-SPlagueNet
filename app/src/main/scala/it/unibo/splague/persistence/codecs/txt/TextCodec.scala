package it.unibo.splague.persistence.codecs.txt

import it.unibo.splague.model.Scenario
import it.unibo.splague.model.connection.Connection.Edge
import it.unibo.splague.persistence.FileFormat
import it.unibo.splague.persistence.codecs.Encoder

trait TextEncoder[A]:
  def encode(a: A): String

/** Provides generic TXT-based implementations for the framework's persistence `Encoder` interface,
  * delegating the actual string formatting to underlying [[TextEncoder]] typeclasses.
  */
object TextCodec:

  /** Bridges the framework's generic [[Encoder]] for [[FileFormat.Txt]] with an underlying
    * [[TextEncoder]].
    *
    * @tparam A
    *   the type of the domain entity to encode
    * @param tEncoder
    *   the underlying domain text encoder for type `A`
    * @return
    *   an instance of [[Encoder]] specialized for [[FileFormat.Txt]]
    */
  given [A](using tEncoder: TextEncoder[A]): Encoder[A, FileFormat.Txt.type] with
    override def encode(a: A): String = tEncoder.encode(a)
