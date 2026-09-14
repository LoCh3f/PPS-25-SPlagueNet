package it.unibo.splague.model

/** A network's cumulative awareness of a malware's presence, constrained to [0.0, 1.0].
  */
opaque type Awareness = Double

object Awareness:

  val none: Awareness = 0.0

  def apply(value: Double): Either[String, Awareness] =
    Either.cond(value >= 0.0 && value <= 1.0, value, s"Awareness must be in [0,1], got $value")

  def clamped(value: Double): Awareness = value.max(0.0).min(1.0)

  extension (a: Awareness)
    def value: Double = a
    def raise(delta: Double): Awareness = clamped(a + delta)
