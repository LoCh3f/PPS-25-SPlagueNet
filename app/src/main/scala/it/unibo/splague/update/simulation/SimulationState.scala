package it.unibo.splague.update.simulation

import it.unibo.splague.model.Scenario

final case class SimulationState(
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
        copy(
          states = remaining,
          current = nextScenario,
          running = remaining.nonEmpty
        )

      case _ =>
        copy(running = false)
