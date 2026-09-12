package it.unibo.splague.update.simulation

import it.unibo.splague.model.Scenario
import it.unibo.splague.update.simulation.event.SimulationEvents.EventSelector

final class SimulationEngine(selector: EventSelector):

  private def step(scenario: Scenario): Scenario =
    val event = selector.nextEvent(scenario)
    val nextScenario = event(scenario)

    nextScenario.copy(
      tick = scenario.tick + 1
    )

  def run(initial: Scenario): LazyList[Scenario] =
    LazyList
      .iterate(initial)(step)
      .takeWhile(scenario => scenario.tick <= scenario.maxIterations)
