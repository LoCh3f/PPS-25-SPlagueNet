package it.unibo.splague.update

import it.unibo.splague.AppState
import it.unibo.splague.update.simulation.report.ScenarioReport
import it.unibo.splague.utils.SimpleScenario
import it.unibo.splague.view.{Screen, ValidationError}
import it.unibo.splague.view.form.ScenarioForm

/** Handles screen navigation and report import: [[Msg.GoToMenu]], [[Msg.GoToSimulation]],
  * [[Msg.GoToReport]], [[Msg.ImportReport]].
  */
// $COVERAGE-OFF$
object NavigationUpdate:

  def update(msg: Msg, state: AppState): AppState = msg match

    case Msg.GoToMenu =>
      state.copy(screen = Screen.Menu)

    case Msg.GoToSimulation =>
      state.scenarioForm match
        case Some(_) =>
          state.copy(screen = Screen.Simulation)

        case None =>
          val scenario =
            state.model.currentScenario
              .map(Right(_))
              .getOrElse(SimpleScenario.linearScenario())

          scenario match
            case Right(s) =>
              state.copy(
                screen = Screen.Simulation,
                scenarioForm = Some(ScenarioForm.fromScenario(s))
              )

            case Left(error) =>
              state.copy(
                errors = Vector(ValidationError("scenario", error))
              )

    case Msg.GoToReport =>
      state.simulation match
        case Some(simulation) if !simulation.running =>
          state.copy(
            screen = Screen.Report,
            report = Some(ScenarioReport.from(simulation.initial, simulation.selector)),
            errors = Vector.empty
          )

        case _ =>
          state.copy(screen = Screen.Report, errors = Vector.empty)

    case Msg.ImportReport(report) =>
      state.copy(report = Some(report), errors = Vector.empty)
// $COVERAGE-ON$
