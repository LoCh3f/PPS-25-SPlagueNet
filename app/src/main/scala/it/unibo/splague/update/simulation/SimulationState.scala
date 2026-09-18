package it.unibo.splague.update.simulation

import it.unibo.splague.model.Scenario
import it.unibo.splague.update.simulation.event.SimulationEvents.EventSelector

final case class SimulationState(
    initial: Scenario,
    selector: EventSelector,
    states: LazyList[Scenario],
    current: Scenario,
    running: Boolean
):

  def currentTick: Int =
    current.tick

  def isFinished: Boolean =
    states.isEmpty || current.tick >= current.maxIterations

  def next: SimulationState =
    states match
      case nextScenario #:: remaining =>
        copy(states = remaining, current = nextScenario, running = remaining.nonEmpty)
      case _ =>
        copy(running = false)
