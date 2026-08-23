package it.unibo.splague.model

object Probability:
  opaque type Probability = Double

  def apply(value: Double): Either[String, Probability] =
    Either.cond(value >= 0.0 && value <= 1.0, value, s"Probability must be in [0,1], got $value")

  extension (p: Probability) def value: Double = p
