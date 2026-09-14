package it.unibo.splague.view.form

import it.unibo.splague.model.Awareness
import it.unibo.splague.model.Awareness.*

final case class AwarenessForm(
    value: String
)

object AwarenessForm:

  def fromDomain(awareness: Awareness): AwarenessForm =
    AwarenessForm(
      value = awareness.value.toString
    )

  def toDomain(form: AwarenessForm): Either[String, Double] =
    form.value.trim.toDoubleOption.toRight(
      s"Awareness must be a valid number, got '${form.value}'"
    )
