package it.unibo.splague.update

import it.unibo.splague.AppState
import it.unibo.splague.view.ValidationError
import it.unibo.splague.view.form.ScenarioForm

/** Handles switching, saving, and discarding edits to a saved scenario: [[Msg.SelectScenario]],
  * [[Msg.SaveScenario]], [[Msg.CancelScenario]].
  */
// $COVERAGE-OFF$
object ScenarioLifecycleUpdate:

  def update(msg: Msg, state: AppState): AppState = msg match

    case Msg.SelectScenario(name) =>
      if state.simulation.exists(_.running) then
        state.copy(
          errors = Vector(
            ValidationError("simulation", "Cannot switch scenario while the simulation is running")
          )
        )
      else
        state.model.scenarios.find(_.name == name) match
          case Some(scenario) =>
            // Loading a different scenario discards any run (and report) tied to the previous
            // one: it belongs to a scenario no longer open, so keeping it around would let Report
            // or Reset silently act on the wrong scenario.
            state.copy(
              model = state.model.copy(currentScenario = Some(scenario)),
              scenarioForm = Some(ScenarioForm.fromScenario(scenario)),
              simulation = None,
              report = None,
              errors = Vector.empty
            )

          case None =>
            state.copy(
              errors = Vector(ValidationError("scenario", s"No scenario named '$name' found"))
            )

    case Msg.SaveScenario =>
      saveScenario(state)

    case Msg.CancelScenario =>
      state.model.currentScenario match
        case Some(saved) =>
          state.copy(
            scenarioForm = Some(ScenarioForm.fromScenario(saved)),
            errors = Vector.empty
          )

        case None =>
          state.copy(errors = Vector.empty)

  private def saveScenario(state: AppState): AppState =
    state.scenarioForm match

      case None =>
        state.copy(
          errors = Vector(
            ValidationError(
              "scenarioForm",
              "No scenario form is open"
            )
          )
        )

      case Some(form) =>
        ScenarioForm.toDomain(form) match

          case Left(error) =>
            state.copy(
              errors = Vector(
                ValidationError("scenario", error)
              )
            )

          case Right(updatedScenario) =>
            // Both the scenario and its malware are upserted by their own (new) name only: saving
            // again under the same name updates that entry in place, but saving under a changed
            // name is a "Save As" — it leaves the old entry untouched and adds a new one, rather
            // than silently renaming the existing saved entry out from under the user.
            val updatedModel =
              state.model
                .upsertScenario(updatedScenario)
                .upsertMalware(updatedScenario.virus)
                .copy(currentScenario = Some(updatedScenario))

            state.copy(
              model = updatedModel,
              scenarioForm = Some(ScenarioForm.fromScenario(updatedScenario)),
              errors = Vector.empty
            )
// $COVERAGE-ON$
