package it.unibo.splague.simulation.event

import it.unibo.splague.simulation.Scenario
import it.unibo.splague.simulation.event.SimulationEvents.Event
import it.unibo.splague.update.AwarenessRules

object Detection extends Event:
  override def apply(scenario: Scenario): Scenario =
    val signal = AwarenessRules.detectionSignal(scenario.topology, scenario.virus)
    scenario.copy(awareness = scenario.awareness.raise(signal))
