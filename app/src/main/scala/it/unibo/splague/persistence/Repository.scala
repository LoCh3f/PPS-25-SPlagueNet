package it.unibo.splague.persistence

import it.unibo.splague.persistence.codecs.{Decoder, Encoder}

import java.nio.file.{Files, Path}
import scala.util.Try

trait Writer[A]:
  def save(a: A, path: Path): Either[PersistenceError, Path]

trait Reader[A]:
  def load(path: Path): Either[PersistenceError, A]

trait Repository[A] extends Writer[A] with Reader[A]

object Repository:
  def json[A](using
      enc: Encoder[A, FileFormat.Json.type],
      dec: Decoder[A, FileFormat.Json.type]
  ): Repository[A] =
    new Repository[A]:

      def save(a: A, path: Path): Either[PersistenceError, Path] =
        Try(Files.writeString(path, enc.encode(a))).toEither.left
          .map(e => PersistenceError.IO(e.getMessage))
          .map(_ => path)

      def load(path: Path): Either[PersistenceError, A] =
        for
          raw <- Try(Files.readString(path)).toEither.left.map(e =>
            PersistenceError.IO(e.getMessage)
          )
          a <- dec.decode(raw)
        yield a

  def txtWriter[A](using enc: Encoder[A, FileFormat.Txt.type]): Writer[A] =
    new Writer[A]:
      def save(a: A, path: Path): Either[PersistenceError, Path] =
        Try(Files.writeString(path, enc.encode(a))).toEither.left
          .map(e => PersistenceError.IO(e.getMessage))
          .map(_ => path)
