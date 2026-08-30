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

  class SimulationEngineState private (private val list: LazyList[Scenario]):
    def remaining: LazyList[Scenario] = list

    def nonEmpty: Boolean = list.nonEmpty

    def advance(): SimulationEngineState =
      if list.nonEmpty then SimulationEngineState(list.tail)
      else this

    override def hashCode(): Int = remaining.hashCode()

    override def toString(): String = s"SimulationEngineState($remaining)"

    override def equals(obj: Any): Boolean = obj match
      case that: SimulationEngineState => this.remaining == that.remaining
      case _                           => false

  object SimulationEngineState:
    def apply(list: LazyList[Scenario]): SimulationEngineState =
      new SimulationEngineState(list.to(LazyList))

  enum Screen:
    case Menu
    case Simulation(engineState: SimulationEngineState)
    case ConfirmScenario
    case SelectScenario

  case class ModelState(screen: Screen)

  // TODO define all encompassing event(infection, cure, destruction?)
  private val selector: EventSelector = _ => Infection.InfectionEvent

  private val engine = SimulationEngine(selector)

  def update(msg: Msg, modelState: ModelState): ModelState =
    (msg, modelState.screen) match
//      case (Msg.GoToSimulation, Screen.Menu) =>
//        modelState.copy(screen = Screen.Simulation(SimulationEngineState(LazyList())))
//      case (Msg.Step, Screen.Simulation(engineState)) =>
//        engineState.remaining match
//          case rest if rest.nonEmpty =>
//            val nextEngineState = SimulationEngineState(rest.tail)
//            modelState.copy(screen = Simulation(nextEngineState))
//          case _ => modelState
      case (Msg.ReturnToMenu, _) =>
        modelState.copy(screen = Screen.Menu)
      case _ => modelState

  object ModelState:
    def init(): ModelState = ModelState(screen = Screen.Menu)
