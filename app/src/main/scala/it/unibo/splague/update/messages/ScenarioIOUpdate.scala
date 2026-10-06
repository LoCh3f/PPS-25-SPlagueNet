package it.unibo.splague.update.messages

import it.unibo.splague.AppState
import it.unibo.splague.model.Scenario
import it.unibo.splague.persistence.FileFormat.{Json, Txt}
import it.unibo.splague.persistence.{ExportPaths, FileFormat}
import it.unibo.splague.update.Msg
import it.unibo.splague.view.ValidationError
import it.unibo.splague.view.form.ScenarioForm

import java.nio.file.Files
import scala.util.Try

/** Handles exporting and importing a scenario to/from disk: [[Msg.ExportScenario]],
  * [[Msg.ImportScenario]].
  */
// $COVERAGE-OFF$
object ScenarioIOUpdate:

  def update(msg: Msg, state: AppState): AppState = (msg: @unchecked) match

    case Msg.ExportScenario(format) =>
      resolveScenarioToExport(state) match
        case Left(error) =>
          state.copy(errors = Vector(ValidationError("export", "Unable to export scenario")))
        case Right(scenarioToExport) =>
          val path = format match {
            case Json => ExportPaths.pathFor(scenarioToExport.name, FileFormat.Json)
            case Txt  => ExportPaths.pathFor(scenarioToExport.name, FileFormat.Txt)
          }

          val res: Either[String, Unit] = for
            _ <- Try(Files.createDirectories(ExportPaths.baseDirectory)).toEither.left.map(e =>
              s"Unable to create dir: ${e.getMessage}"
            )

            _ <- format match
              case FileFormat.Json =>
                AppState.defaultScenarioJsonRepository
                  .save(scenarioToExport, path)
                  .left
                  .map(_.toString)
              case FileFormat.Txt =>
                AppState.defaultScenarioTxtWriter.save(scenarioToExport, path).left.map(_.toString)
          yield ()

          res match
            case Left(err) =>
              state.copy(errors = Vector(ValidationError("export", err)))
            case Right(_) =>
              state

    case Msg.ImportScenario(format, path) =>
      format match
        case FileFormat.Json =>
          AppState.defaultScenarioJsonRepository.load(path) match
            case Left(error) =>
              state.copy(errors = Vector(ValidationError("import", error.toString)))

            case Right(scenarioToImport) =>
              state.copy(
                model = state.model.copy(currentScenario = Some(scenarioToImport)),
                scenarioForm = Some(ScenarioForm.fromScenario(scenarioToImport))
              )

        case _ =>
          state.copy(errors =
            Vector(ValidationError("import", "File format not yet supported for import"))
          )

  private def resolveScenarioToExport(state: AppState): Either[String, Scenario] =
    state.simulation match
      case Some(sim) =>
        Right(sim.current)

      case None =>
        state.model.currentScenario match
          case Some(scenario) => Right(scenario)
          case None =>
            state.scenarioForm match
              case Some(scenarioForm) => ScenarioForm.toDomain(scenarioForm)
              case None               => Left("No scenario to export available!")
// $COVERAGE-ON$
