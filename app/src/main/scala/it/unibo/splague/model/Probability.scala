package it.unibo.splague.model

/** A probability value constrained to the range [0.0, 1.0].
  *
  * This is an opaque type: outside this file, `Probability` is a distinct, unrelated type to
  * `Double`, so you cannot accidentally pass a raw Double where a validated Probability is
  * expected. Construct one via `Probability(value)` (validated) or `Probability.clamped(value)`
  * (silently clamped to the valid range).
  */
opaque type Probability = Double

object Probability:

  /** Validates that `value` lies in [0.0, 1.0]; fails otherwise. */
  def apply(value: Double): Either[String, Probability] =
    Either.cond(value >= 0.0 && value <= 1.0, value, s"Probability must be in [0,1], got $value")

  extension (p: Probability) def value: Double = p
