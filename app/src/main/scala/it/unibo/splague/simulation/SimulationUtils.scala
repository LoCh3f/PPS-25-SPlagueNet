package it.unibo.splague.simulation

object SimulationUtils:

  trait Event:
    def apply(scenario: Scenario): Scenario

  trait EventSelector:
    def nextEvent(scenario: Scenario): Event
