package it.unibo.splague.model.countermeasures

case class CountermeasureConfig(
    activeCountermeasures: Set[Countermeasures],
    countermeasureLevels: Map[Double, Countermeasures]
)
object CountermeasureConfig:
  val empty: CountermeasureConfig = CountermeasureConfig(Set.empty, Map.empty).toOption.get

  def apply(
      activeCountermeasures: Set[Countermeasures],
      countermeasureLevels: Map[Double, Countermeasures]
  ): Either[String, CountermeasureConfig] =
    if countermeasureLevels.keys.exists(threshold => threshold < 0.0 || threshold > 1.0) then
      Left("Thresholds in countermeasureLevels must be between 0.0 and 1.0")
    else Right(new CountermeasureConfig(activeCountermeasures, countermeasureLevels))
