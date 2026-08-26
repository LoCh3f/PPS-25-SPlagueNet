package it.unibo.splague.simulation
import it.unibo.splague.simulation.SimulationUtils.EventSelector

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
