package it.unibo.splague.model.countermeasures

case class CountermeasureConfig(
    activeCountermeasures: Set[Countermeasures],
    countermeasureLevels: Map[Double, Countermeasures],
    patchBoostAmount: Double,
    defenseBoostAmount: Double,
    patchCureProbability: Double
)
object CountermeasureConfig:
  val defaultPatchBoostAmount: Double = 0.05
  val defaultDefenseBoostAmount: Double = 0.05
  val defaultPatchCureProbability: Double = 0.5
  val empty: CountermeasureConfig = CountermeasureConfig(
    Set.empty,
    Map.empty,
    defaultPatchBoostAmount,
    defaultDefenseBoostAmount,
    defaultPatchCureProbability
  ).toOption.get

  def apply(
      activeCountermeasures: Set[Countermeasures] = Set.empty,
      countermeasureLevels: Map[Double, Countermeasures] = Map.empty,
      patchBoostAmount: Double = defaultPatchBoostAmount,
      defenseBoostAmount: Double = defaultDefenseBoostAmount,
      patchCureProbability: Double = defaultPatchCureProbability
  ): Either[String, CountermeasureConfig] =
    if countermeasureLevels.keys.exists(threshold => threshold < 0.0 || threshold > 1.0) then
      Left("Thresholds in countermeasureLevels must be between 0.0 and 1.0")
    else if defenseBoostAmount < 0.0 || defenseBoostAmount > 1.0 ||
      patchBoostAmount < 0.0 || patchBoostAmount > 1.0 ||
      patchCureProbability < 0.0 || patchCureProbability > 1.0
    then Left("Boost amounts and cure probability must be between 0.0 and 1.0")
    else
      Right(
        new CountermeasureConfig(
          activeCountermeasures,
          countermeasureLevels,
          patchBoostAmount,
          defenseBoostAmount,
          patchCureProbability
        )
      )
