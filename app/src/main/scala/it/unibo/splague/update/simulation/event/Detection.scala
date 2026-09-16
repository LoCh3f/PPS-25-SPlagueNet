package it.unibo.splague.update.simulation.event

import it.unibo.splague.model.{Awareness, Scenario}
import SimulationEvents.Event
import it.unibo.splague.update.simulation.event.rules.AwarenessRules

/** Event that simulates malware detection across the network. Computes a detection signal based on
  * network topology and malware traits, then raises the overall awareness level in the scenario
  * accordingly.
  */
object Detection extends Event:
  /** Executes the detection event on a scenario.
    *
    * @param scenario
    *   the current simulation scenario
    * @return
    *   updated scenario with raised awareness level
    */
  override def apply(scenario: Scenario): Scenario =
    val signal = AwarenessRules.detectionSignal(scenario.topology, scenario.virus)
    scenario.copy(awareness = scenario.awareness.trackTowards(signal, Awareness.defaultRate))
