package it.unibo.splague.persistence

import it.unibo.splague.persistence.FileFormat

import java.nio.file.{Files, Path}
import scala.util.Try

enum PersistenceError:
  case IO(message: String)
  case Parsing(message: String)

sealed trait FileFormat
object FileFormat:
  sealed trait Json extends FileFormat
  sealed trait Txt extends FileFormat
  sealed trait Html extends FileFormat

trait Encoder[A, F <: FileFormat]:
  def encode(a: A): String

trait Decoder[A, F <: FileFormat]:
  def decode(raw: String): Either[PersistenceError, A]
