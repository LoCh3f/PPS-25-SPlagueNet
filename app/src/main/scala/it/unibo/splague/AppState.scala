package it.unibo.splague

import it.unibo.splague.model.{ModelState, Scenario}
import it.unibo.splague.persistence.Repository
import it.unibo.splague.update.simulation.SimulationState
import it.unibo.splague.view.form.ScenarioForm
import it.unibo.splague.view.{Screen, ValidationError}
import it.unibo.splague.persistence.codecs.json.CodecCatalog.given
import it.unibo.splague.persistence.codecs.json.JsonCodec.given

// $COVERAGE-OFF$
final case class AppState(
    model: ModelState,
    screen: Screen = Screen.Menu,
    scenarioForm: Option[ScenarioForm] = None,
    simulation: Option[SimulationState] = None,
    errors: Vector[ValidationError] = Vector.empty
)
object AppState:
  def init(model: ModelState): AppState =
    AppState(
      model = model
    )

  def defaultScenarioJsonRepository: Repository[Scenario] =
    Repository.json[Scenario]
// $COVERAGE-ON$
