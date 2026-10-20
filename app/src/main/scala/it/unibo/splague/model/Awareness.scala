package it.unibo.splague.model

import it.unibo.splague.utils.config.SimulationConfig.Bounds
import it.unibo.splague.utils.config.UIConfig.ErrorMessages

/** A network's cumulative awareness of a malware's presence, constrained to [0.0, 1.0].
  */
opaque type Awareness = Double

object Awareness:

  val none: Awareness = Bounds.MIN_AWARENESS

  val defaultRate: Double = 0.3

  def apply(value: Double): Either[String, Awareness] =
    Either.cond(
      value >= Bounds.MIN_AWARENESS && value <= Bounds.MAX_AWARENESS,
      value,
      ErrorMessages.awarenessOutOfBounds(value)
    )

  def clamped(value: Double): Awareness = value.max(Bounds.MIN_AWARENESS).min(Bounds.MAX_AWARENESS)

  extension (a: Awareness)
    def value: Double = a
    def raise(delta: Double): Awareness = clamped(a + delta)

    /** Moves awareness toward `signal`, at `rate` per tick (0,1]. Fixed point is signal itself —
      * awareness never overshoots it.
      */
    def trackTowards(signal: Double, rate: Double): Awareness =
      clamped(a + rate * (signal - a))
