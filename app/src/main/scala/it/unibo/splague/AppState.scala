package it.unibo.splague

import it.unibo.splague.model.ModelState
import it.unibo.splague.update.simulation.SimulationState
import it.unibo.splague.update.simulation.report.ScenarioReport
import it.unibo.splague.view.form.ScenarioForm
import it.unibo.splague.view.{Screen, ValidationError}

final case class AppState(
    model: ModelState,
    screen: Screen = Screen.Menu,
    scenarioForm: Option[ScenarioForm] = None,
    simulation: Option[SimulationState] = None,
    report: Option[ScenarioReport] = None,
    errors: Vector[ValidationError] = Vector.empty
)
object AppState:
  def init(model: ModelState): AppState =
    AppState(
      model = model
    )
