package it.unibo.splague.model

import it.unibo.splague.model.malware.Malware

final case class ModelState(
    scenarios: Vector[Scenario] = Vector.empty,
    malwares: Vector[Malware] = Vector.empty,
    currentScenario: Option[Scenario] = None
):
  def addScenario(scenario: Scenario): ModelState =
    copy(scenarios = scenarios :+ scenario)

  def addMalware(malware: Malware): ModelState =
    copy(malwares = malwares :+ malware)

  def selectScenario(scenario: Scenario): Either[String, ModelState] =
    if scenarios.contains(scenario) then Right(copy(currentScenario = Some(scenario)))
    else Left("The scenario is not present in the model state")

  def selectScenarioByName(name: String): Either[String, ModelState] =
    scenarios.find(_.name == name) match
      case Some(scenario) => Right(copy(currentScenario = Some(scenario)))
      case None           => Left(s"Scenario '$name' not found")

  def clearCurrentScenario: ModelState =
    copy(currentScenario = None)
