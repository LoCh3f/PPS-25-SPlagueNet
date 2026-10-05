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
            // Both the scenario and its malware are upserted by name (their previous names, from
            // the scenario open before this edit, included) so saving a scenario that was edited,
            // renamed, run, or reset since it was last saved still lands back in the same slots
            // instead of leaving stale duplicates behind (see ModelState.upsertScenario).
            val previousScenarioName = state.model.currentScenario.map(_.name)
            val previousMalwareName = state.model.currentScenario.map(_.virus.name)

            val updatedModel =
              state.model
                .upsertScenario(updatedScenario, previousScenarioName)
                .upsertMalware(updatedScenario.virus, previousMalwareName)
                .copy(currentScenario = Some(updatedScenario))

            state.copy(
              model = updatedModel,
              scenarioForm = Some(ScenarioForm.fromScenario(updatedScenario)),
              errors = Vector.empty
            )
// $COVERAGE-ON$
