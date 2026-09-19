package it.unibo.splague.persistence.codecs

import it.unibo.splague.persistence.{FileFormat, PersistenceError}

trait Encoder[A, F <: FileFormat]:
  def encode(a: A): String

trait Decoder[A, F <: FileFormat]:
  def decode(raw: String): Either[PersistenceError, A]
