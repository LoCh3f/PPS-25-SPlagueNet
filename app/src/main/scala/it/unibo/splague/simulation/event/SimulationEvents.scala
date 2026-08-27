package it.unibo.splague.simulation.event

import it.unibo.splague.simulation.Scenario

object SimulationEvents:

  trait Event:
    def apply(scenario: Scenario): Scenario

  trait EventSelector:
    def nextEvent(scenario: Scenario): Event
