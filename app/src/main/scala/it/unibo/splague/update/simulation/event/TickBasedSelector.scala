package it.unibo.splague.update.simulation.event

import it.unibo.splague.model.Scenario
import SimulationEvents.{Event, EventSelector}

final class TickBasedSelector(events: Vector[Event]) extends EventSelector:

  override def nextEvent(scenario: Scenario): Event =
    if events.isEmpty then (s: Scenario) => s
    else events(scenario.tick % events.size)

object TickBasedCyclicSelector:
  def apply(e1: Event, e2: Event, e3: Event, e4: Event): EventSelector =
    new TickBasedSelector(Vector(e1, e2, e3, e4))
