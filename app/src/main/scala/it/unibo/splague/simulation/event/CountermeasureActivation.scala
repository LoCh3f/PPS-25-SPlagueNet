package it.unibo.splague.simulation.event

import it.unibo.splague.simulation.Scenario

object CountermeasureActivation:
  object ActivationEvent extends SimulationEvents.Event:
    override def apply(scenario: Scenario): Scenario =
      val config = scenario.countermeasureConfig
      val currentAwareness = scenario.awareness.value

      val toBeActivated = config.countermeasureLevels
        .filter((threshold, counter) =>
          currentAwareness >= threshold & !config.activeCountermeasures.contains(counter)
        )
        .values
        .toSet

      val updatedConfig = config.copy(
        activeCountermeasures = config.activeCountermeasures ++ toBeActivated
      )
      scenario.copy(countermeasureConfig = updatedConfig)
