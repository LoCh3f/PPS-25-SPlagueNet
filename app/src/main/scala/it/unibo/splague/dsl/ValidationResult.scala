package it.unibo.splague.dsl

/** Result of validating a DSL declaration: `Right` on success, or `Left` with every accumulated
  * error message on failure. Unlike a plain `Either` -based `for` -comprehension, errors from
  * independent declarations (e.g. multiple malformed node IDs) are collected together rather than
  * short-circuiting on the first failure.
  */
type ValidationResult[A] = Either[List[String], A]

object ValidationResult:
  /** Splits a list of independently-validated results into all accumulated error messages and all
    * successfully-produced values, preserving order within each side.
    */
  def partition[A](results: List[ValidationResult[A]]): (List[String], List[A]) =
    (results.collect { case Left(e) => e }.flatten, results.collect { case Right(a) => a })
