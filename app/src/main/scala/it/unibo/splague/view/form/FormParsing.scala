package it.unibo.splague.view.form

import it.unibo.splague.model.Probability

/** Shared parsing utilities for form-to-domain conversions. Every method returns
  * [[Either[String, A]]] with a user-readable error message naming the field that failed.
  */
object FormParsing:

  def parseDouble(s: String, field: String): Either[String, Double] =
    s.trim.toDoubleOption.toRight(s"$field must be a number, got '$s'")

  def parseInt(s: String, field: String): Either[String, Int] =
    s.trim.toIntOption.toRight(s"$field must be an integer, got '$s'")

  /** Validates the value is a probability in [0,1]; the error message names the field and quotes
    * the domain error.
    */
  def parseProbability(s: String, field: String): Either[String, Probability] =
    parseDouble(s, field).flatMap { value =>
      Probability(value).left.map(error => s"$field: $error")
    }

  /** Left-folds over `items`, threading an accumulator through `f`. Short-circuits on the first
    * failure, reporting the offending item. Use this when ordering matters and the first error is
    * enough.
    */
  def foldEither[A, B](
      initial: Vector[A],
      items: Vector[B]
  )(f: (Vector[A], B) => Either[String, Vector[A]]): Either[String, Vector[A]] =
    items.foldLeft(Right(initial): Either[String, Vector[A]]) { (result, item) =>
      result.flatMap(acc => f(acc, item))
    }

  /** Checks that no two nodes in `nodes` share the same id. Returns [[Right(())]] when ids are
    * unique, [[Left(message)]] otherwise.
    */
  def checkDuplicateNodeIds(nodes: Vector[it.unibo.splague.model.node.Node]): Either[String, Unit] =
    val duplicated =
      nodes
        .map(_.nodeId.value)
        .groupBy(identity)
        .collect { case (id, occ) if occ.size > 1 => id }
    if duplicated.nonEmpty then Left(s"Duplicated node IDs: ${duplicated.mkString(", ")}")
    else Right(())
