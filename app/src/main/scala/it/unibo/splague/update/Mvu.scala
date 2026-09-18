package it.unibo.splague.update

import it.unibo.splague.AppState
import it.unibo.splague.AppState.defaultScenarioJsonRepository
import it.unibo.splague.model.node.{NodeId, NodeState}
import it.unibo.splague.model.Scenario
import it.unibo.splague.persistence.{ExportPaths, FileFormat}
import it.unibo.splague.update.simulation.event.SimulationEvents.{Event, EventSelector}
import it.unibo.splague.update.simulation.{SimulationEngine, SimulationState}
import it.unibo.splague.update.simulation.event.{
  CountermeasureActivation,
  Cure,
  Defense,
  Destroy,
  Detection,
  Infection,
  Prevention,
  SimulationEvents,
  TickBasedCyclicSelector
}
import it.unibo.splague.update.simulation.report.ScenarioReport
import it.unibo.splague.utils.SimpleScenario
import it.unibo.splague.view.{Screen, ValidationError}
import it.unibo.splague.view.form.{AwarenessForm, ScenarioForm}

import java.nio.file.Files
import scala.util.Try

// $COVERAGE-OFF$
object Mvu:

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

        case Some(_) =>
          state.copy(
            errors = Vector(ValidationError("simulation", "Simulation is still running"))
          )

        case None =>
          state.copy(
            errors = Vector(ValidationError("simulation", "No simulation to report on"))
          )

    case Msg.UpdateScenarioName(form) =>
      updateForm(state) { s =>
        s.copy(
          name = form.name,
          seed = form.seed,
          maxIterations = form.maxIterations,
          startingNodeId = form.startingNodeId
        )
      }

    case Msg.AddNode(node) =>
      updateForm(state)(s => s.copy(topology = s.topology.copy(nodes = s.topology.nodes :+ node)))

    case Msg.UpdateNode(node) =>
      updateForm(state) { form =>
        form.copy(
          topology = form.topology.copy(
            nodes = form.topology.nodes.map { n =>
              if n.id == node.id then node else n
            }
          )
        )
      }

    case Msg.RemoveNode(nodeId) =>
      updateForm(state) { form =>
        val normalized = NodeId.normalize(nodeId)

        form.copy(
          topology = form.topology.copy(
            nodes = form.topology.nodes.filterNot { node =>
              NodeId.normalize(node.id) == normalized
            },
            edges = form.topology.edges.filter { edge =>
              NodeId.normalize(edge.from) != normalized &&
              NodeId.normalize(edge.to) != normalized
            }
          )
        )
      }
    case Msg.AddEdge(edge) =>
      updateForm(state)(s => s.copy(topology = s.topology.copy(edges = s.topology.edges :+ edge)))
    case Msg.UpdateEdge(edge) =>
      updateForm(state) { form =>
        form.copy(
          topology = form.topology.copy(
            edges = form.topology.edges.map { e =>
              if (e.from == edge.from && e.to == edge.to) then edge else e
            }
          )
        )
      }
    case Msg.RemoveEdge(edge) =>
      updateForm(state) { form =>
        form.copy(
          topology = form.topology.copy(
            edges = form.topology.edges.filterNot(e => e.to == edge.to && e.from == edge.from)
          )
        )
      }
    case Msg.UpdateMalware(malware) =>
      updateForm(state)(s => s.copy(virus = malware))

    case Msg.UpdateAwareness(awareness) =>
      state.scenarioForm match
        case None =>
          state.copy(
            errors = Vector(
              ValidationError("scenarioForm", "No scenario form is open")
            )
          )

        case Some(form) =>
          AwarenessForm.toDomain(awareness) match
            case Left(error) =>
              state.copy(errors = Vector(ValidationError("awareness", error)))

            case Right(value) =>
              state.copy(
                scenarioForm = Some(form.copy(awareness = value)),
                errors = Vector.empty
              )

    case Msg.UpdateCountermeasure(countermeasure) =>
      updateForm(state)(s => s.copy(countermeasureConfig = countermeasure))

    case Msg.SelectScenario(name) =>
      state.model.scenarios.find(_.name == name) match
        case Some(scenario) =>
          state.copy(
            model = state.model.copy(currentScenario = Some(scenario)),
            scenarioForm = Some(ScenarioForm.fromScenario(scenario)),
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

    case Msg.StartSimulation =>
      state.scenarioForm match
        case None =>
          state.copy(
            errors = Vector(
              ValidationError("scenarioForm", "No scenario form is open")
            )
          )

        case Some(form) =>
          ScenarioForm.toDomain(form) match
            case Left(error) =>
              state.copy(errors = Vector(ValidationError("scenario", error)))

            case Right(scenario) =>
              val seeded = seedOutbreak(scenario)
              val states =
                new SimulationEngine(simulationSelector).run(seeded)

              states match
                case current #:: upcoming =>
                  state.copy(
                    simulation = Some(
                      SimulationState(
                        initial = seeded,
                        selector = simulationSelector,
                        states = upcoming,
                        current = current,
                        running = upcoming.nonEmpty
                      )
                    ),
                    model = state.model.copy(currentScenario = Some(current)),
                    errors = Vector.empty
                  )

                case _ =>
                  state.copy(
                    errors = Vector(
                      ValidationError("scenario", "Unable to start the simulation")
                    )
                  )

    case Msg.SimulationStep =>
      state.simulation match

        case Some(simulation) if simulation.running =>
          val nextSimulation =
            simulation.next

          state.copy(
            simulation = Some(nextSimulation),
            model = state.model.copy(
              currentScenario = Some(nextSimulation.current)
            ),
            scenarioForm = Some(ScenarioForm.fromScenario(nextSimulation.current)),
            errors = Vector.empty
          )

        case _ => state

    case Msg.ExportScenario(format) =>
      resolveScenarioToExport(state) match
        case Left(error) =>
          state.copy(errors = Vector(ValidationError("export", "Unable to export scenario")))
        case Right(scenario) =>
          val path = ExportPaths.pathFor(scenario.name)

          val res: Either[String, Unit] = for
            _ <- Try(Files.createDirectories(ExportPaths.baseDirectory)).toEither.left.map(e =>
              s"Unable to create dir: ${e.getMessage}"
            )

            _ <- format match
              case FileFormat.Json =>
                AppState.defaultScenarioJsonRepository.save(scenario, path).left.map(_.toString)
              case other =>
                Left(s"File format not supported yet: $other")
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

            case Right(scenario) =>
              state.copy(
                model = state.model.copy(currentScenario = Some(scenario)),
                scenarioForm = Some(ScenarioForm.fromScenario(scenario))
              )

        case _ =>
          state.copy(errors =
            Vector(ValidationError("import", "File format not yet supported for import"))
          )

  /** Marks the scenario's starting node as the outbreak's patient zero. A scenario's `startingNode`
    * is only a topology reference; nothing else ever infects it, so without this every node stays
    * `Healthy` forever and the simulation runs to completion doing nothing observable.
    */
  private def seedOutbreak(scenario: Scenario): Scenario =
    val startingId = scenario.startingNode.nodeId.value

    scenario.topology.nodes.get(startingId) match
      case Some(node) if node.state != NodeState.Infected =>
        val infected = node.copy(state = NodeState.Infected)

        scenario.copy(
          topology = scenario.topology.copy(
            nodes = scenario.topology.nodes.updated(startingId, infected)
          ),
          startingNode = infected
        )

      case _ => scenario

  /** Cycles one composite event per tick (tick % 4): awareness/activation, containment defenses,
    * infection spread + prevention boosts, then cure/destroy. Groups the 7 event categories into 4
    * slots since [[TickBasedCyclicSelector]] cycles a fixed set of 4 events, one per tick.
    */
  private def simulationSelector: EventSelector =
    TickBasedCyclicSelector(
      combine(Detection, CountermeasureActivation.ActivationEvent),
      combine(Defense.IsolationEvent, Defense.FirewallEvent),
      combine(Infection.InfectionEvent, Prevention.PatchBoostEvent, Prevention.DefenseBoostEvent),
      combine(
        Cure.CureEvent,
        Cure.LowerWorkloadEvent,
        Destroy.IncreaseWorkloadEvent,
        Destroy.DestroyEvent
      )
    )

  private def combine(events: Event*): Event =
    (scenario: Scenario) => events.foldLeft(scenario)((acc, event) => event(acc))

  private def updateForm(
      state: AppState
  )(change: ScenarioForm => ScenarioForm): AppState =
    state.scenarioForm match
      case Some(form) =>
        state.copy(
          scenarioForm = Some(change(form)),
          errors = Vector.empty
        )

      case None =>
        state.copy(
          errors = Vector(
            ValidationError(
              "scenarioForm",
              "No scenario form is open"
            )
          )
        )

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
            val updatedModel =
              state.model.currentScenario match

                case Some(previousScenario) =>
                  state.model.copy(
                    scenarios = state.model.scenarios.map { scenario =>
                      if scenario == previousScenario then updatedScenario
                      else scenario
                    },
                    currentScenario = Some(updatedScenario)
                  )

                case None =>
                  state.model.copy(
                    scenarios = state.model.scenarios :+ updatedScenario,
                    currentScenario = Some(updatedScenario)
                  )

            state.copy(
              model = updatedModel,
              scenarioForm = Some(ScenarioForm.fromScenario(updatedScenario)),
              errors = Vector.empty
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
