package it.unibo.splague.view.form

import it.unibo.splague.model.countermeasures.{CountermeasureConfig, Countermeasures}
import it.unibo.splague.update.{FirewallPolicy, IsolationCriteria}

final case class CountermeasureForm(
    activeCountermeasures: Set[Countermeasures],
    countermeasureLevels: Map[String, Countermeasures],
    patchBoostAmount: String,
    defenseBoostAmount: String,
    patchCureProbability: String,
    isolationCriteria: IsolationCriteria,
    firewallPolicy: FirewallPolicy
)

object CountermeasureForm:

  def fromDomain(
      config: CountermeasureConfig
  ): CountermeasureForm =
    CountermeasureForm(
      activeCountermeasures = config.activeCountermeasures,
      countermeasureLevels = config.countermeasureLevels.map { case (threshold, countermeasure) =>
        threshold.toString -> countermeasure
      },
      patchBoostAmount = config.patchBoostAmount.toString,
      defenseBoostAmount = config.defenseBoostAmount.toString,
      patchCureProbability = config.patchCureProbability.toString,
      isolationCriteria = config.isolationCriteria,
      firewallPolicy = config.firewallPolicy
    )

  def toDomain(
      form: CountermeasureForm
  ): Either[String, CountermeasureConfig] =
    for
      levels <- parseLevels(form.countermeasureLevels)
      patchBoost <- parseDouble(
        form.patchBoostAmount,
        "patchBoostAmount"
      )
      defenseBoost <- parseDouble(
        form.defenseBoostAmount,
        "defenseBoostAmount"
      )
      cureProbability <- parseDouble(
        form.patchCureProbability,
        "patchCureProbability"
      )
      config <- CountermeasureConfig(
        activeCountermeasures = form.activeCountermeasures,
        countermeasureLevels = levels,
        patchBoostAmount = patchBoost,
        defenseBoostAmount = defenseBoost,
        patchCureProbability = cureProbability,
        isolationCriteria = form.isolationCriteria,
        firewallPolicy = form.firewallPolicy
      )
    yield config

  private def parseLevels(
      levels: Map[String, Countermeasures]
  ): Either[String, Map[Double, Countermeasures]] =
    levels.foldLeft(
      Right(Map.empty): Either[String, Map[Double, Countermeasures]]
    ) { case (result, (rawThreshold, countermeasure)) =>
      for
        parsed <- result
        threshold <- parseDouble(
          rawThreshold,
          s"countermeasure threshold '$rawThreshold'"
        )
      yield parsed.updated(threshold, countermeasure)
    }

  private def parseDouble(
      rawValue: String,
      field: String
  ): Either[String, Double] =
    rawValue.trim.toDoubleOption.toRight(
      s"$field must be a valid number, got '$rawValue'"
    )
