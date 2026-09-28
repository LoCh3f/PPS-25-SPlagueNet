package it.unibo.splague.model.countermeasures

import it.unibo.splague.update.{FirewallPolicy, IsolationCriteria}

case class CountermeasureConfig(
    activeCountermeasures: Set[Countermeasures],
    countermeasureLevels: Map[Double, Countermeasures],
    patchBoostAmount: Double,
    defenseBoostAmount: Double,
    patchCureProbability: Double,
    isolationCriteria: IsolationCriteria,
    firewallPolicy: FirewallPolicy
)
object CountermeasureConfig:
  val defaultPatchBoostAmount: Double = 0.05
  val defaultDefenseBoostAmount: Double = 0.05
  val defaultPatchCureProbability: Double = 0.5
  val defaultIsolationCriteria: IsolationCriteria = IsolationCriteria.all
  val defaultFirewallPolicy: FirewallPolicy = FirewallPolicy()

  val empty: CountermeasureConfig = CountermeasureConfig(
    Set.empty,
    Map.empty,
    defaultPatchBoostAmount,
    defaultDefenseBoostAmount,
    defaultPatchCureProbability,
    defaultIsolationCriteria,
    defaultFirewallPolicy
  ).toOption.get

  private def validateLevels(
      levels: Map[Double, Countermeasures]
  ): Either[String, Map[Double, Countermeasures]] =
    Either.cond(
      levels.keys.forall(threshold => threshold >= 0.0 && threshold <= 1.0),
      levels,
      "Thresholds in countermeasureLevels must be between 0.0 and 1.0"
    )

  private def isInRangeZeroOne(field: Double): Either[String, Double] =
    Either.cond(
      field >= 0 && field <= 1.0,
      field,
      "Boost amounts and cure probability must be between 0.0 and 1.0"
    )

  def apply(
      activeCountermeasures: Set[Countermeasures] = Set.empty,
      countermeasureLevels: Map[Double, Countermeasures] = Map.empty,
      patchBoostAmount: Double = defaultPatchBoostAmount,
      defenseBoostAmount: Double = defaultDefenseBoostAmount,
      patchCureProbability: Double = defaultPatchCureProbability,
      isolationCriteria: IsolationCriteria = defaultIsolationCriteria,
      firewallPolicy: FirewallPolicy = defaultFirewallPolicy
  ): Either[String, CountermeasureConfig] = {
    for
      validLevels <- validateLevels(countermeasureLevels)
      validPatchBoostAmount <- isInRangeZeroOne(patchBoostAmount)
      validDefenseBoostAmount <- isInRangeZeroOne(defenseBoostAmount)
      validPatchCureProbability <- isInRangeZeroOne(patchCureProbability)
    yield new CountermeasureConfig(
      activeCountermeasures,
      validLevels,
      validPatchBoostAmount,
      validDefenseBoostAmount,
      validPatchCureProbability,
      isolationCriteria,
      firewallPolicy
    )
  }
