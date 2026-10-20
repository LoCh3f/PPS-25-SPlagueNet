package it.unibo.splague.model

import it.unibo.splague.utils.config.SimulationConfig.Bounds
import it.unibo.splague.utils.config.UIConfig.ErrorMessages

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
    Either.cond(
      value >= Bounds.MIN_PROBABILITY && value <= Bounds.MAX_PROBABILITY,
      value,
      ErrorMessages.probabilityOutOfBounds(value)
    )

  def clamped(value: Double): Probability =
    value.max(Bounds.MIN_PROBABILITY).min(Bounds.MAX_PROBABILITY)

  extension (p: Probability) def value: Double = p
