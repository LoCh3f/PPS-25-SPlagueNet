package it.unibo.splague.simulation.event

import it.unibo.splague.simulation.Scenario
import it.unibo.splague.simulation.event.SimulationEvents.Event
import it.unibo.splague.update.AwarenessRules

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
    // TODO: Once infected nodes filtering is extracted to Topology class,
    // use only infected nodes for detection signal calculation instead of all nodes
    val signal = AwarenessRules.detectionSignal(scenario.topology, scenario.virus)
    scenario.copy(awareness = scenario.awareness.raise(signal))
