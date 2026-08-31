package it.unibo.splague.update

import it.unibo.splague.simulation.event.{Infection, SimulationEvents}
import it.unibo.splague.simulation.{Scenario, SimulationEngine}
import it.unibo.splague.simulation.event.SimulationEvents.EventSelector
import it.unibo.splague.update.Mvu.Screen.Simulation

object Mvu:
  enum Msg:
    case Step
    case ReturnToMenu
    case GoToSimulation
    case GoToCreateView
    case ConfirmScenario
    case GoToSelection
    case LoadTopology
    case SaveScenario

  enum Screen:
    case Menu
    case Simulation(engineState: LazyList[Scenario])
    case ConfirmScenario
    case SelectScenario

  case class ModelState(screen: Screen)

  // TODO define all encompassing event(infection, cure, destruction?)
  private val selector: EventSelector = _ => Infection.InfectionEvent

  private val engine = SimulationEngine(selector)

  def update(msg: Msg, modelState: ModelState): ModelState =
    (msg, modelState.screen) match
      case (Msg.GoToSimulation, Screen.Menu) =>
        modelState.copy(screen = Screen.Simulation(LazyList()))
      case (Msg.Step, Screen.Simulation(engineState)) =>
        engineState.tail match
          case rest if rest.nonEmpty =>
            modelState.copy(screen = Simulation(rest))
          case _ => modelState
      case (Msg.ReturnToMenu, _) =>
        modelState.copy(screen = Screen.Menu)
      case _ => modelState

  object ModelState:
    def init(): ModelState = ModelState(screen = Screen.Menu)
