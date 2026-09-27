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

  /** Adds `scenario` to `scenarios`, replacing whichever existing entry represents the same
    * scenario, or appending it as a new entry if none does. A scenario's name is this model's
    * notion of identity for persistence, so saving a scenario that was edited, run, or reset since
    * it was last saved still lands back in the same slot instead of leaving a stale duplicate
    * behind. Passing `previousName` (the name the scenario had before this edit, e.g.
    * `currentScenario.map(_.name)` from right before the save) additionally recognizes a rename as
    * still being the same entry, replacing the `previousName` -named one instead of leaving it
    * behind as a stale duplicate under its old name; omit it for a scenario that was never saved
    * before.
    */
  def upsertScenario(scenario: Scenario, previousName: Option[String] = None): ModelState =
    copy(scenarios = ModelState.upsertByName(scenarios, scenario, previousName)(_.name))

  /** Adds `malware` to `malwares`, replacing whichever existing entry represents the same malware,
    * or appending it as a new entry if none does. Same rename-aware, upsert-by-name semantics as
    * [[upsertScenario]] — `previousName` is the malware's name before this edit.
    */
  def upsertMalware(malware: Malware, previousName: Option[String] = None): ModelState =
    copy(malwares = ModelState.upsertByName(malwares, malware, previousName)(_.name))

  def selectScenario(scenario: Scenario): Either[String, ModelState] =
    if scenarios.contains(scenario) then Right(copy(currentScenario = Some(scenario)))
    else Left("The scenario is not present in the model state")

  def selectScenarioByName(name: String): Either[String, ModelState] =
    scenarios.find(_.name == name) match
      case Some(scenario) => Right(copy(currentScenario = Some(scenario)))
      case None           => Left(s"Scenario '$name' not found")

  def clearCurrentScenario: ModelState =
    copy(currentScenario = None)

object ModelState:
  /** Shared implementation behind [[ModelState.upsertScenario]] and [[ModelState.upsertMalware]]:
    * drops the `previousName` -named item (if any, and if it's actually being renamed away from),
    * then replaces whatever now shares `item` 's name, or appends `item` if nothing does.
    */
  private def upsertByName[A](items: Vector[A], item: A, previousName: Option[String])(
      nameOf: A => String
  ): Vector[A] =
    val itemName = nameOf(item)

    val staleRemoved = previousName match
      case Some(old) if old != itemName => items.filterNot(a => nameOf(a) == old)
      case _                            => items

    if staleRemoved.exists(a => nameOf(a) == itemName) then
      staleRemoved.map(a => if nameOf(a) == itemName then item else a)
    else staleRemoved :+ item
