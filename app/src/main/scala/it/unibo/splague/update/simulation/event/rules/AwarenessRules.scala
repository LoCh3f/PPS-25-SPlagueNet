package it.unibo.splague.update.simulation.event.rules

import it.unibo.splague.model.malware.Malware
import it.unibo.splague.model.node.{Node, Topology}
import it.unibo.splague.utils.config.SimulationConfig.{Bounds, DetectionFormula}

/** Rules for computing malware detection signals in the network. Detection is based on node
  * workload, node type characteristics, and malware stealth level.
  */
object AwarenessRules:

  /** Calculates detection signal for a single node. Signal = workload � detection coefficient � (1 -
    * stealth)
    *
    * @param node
    *   the node to calculate signal for
    * @param malware
    *   the malware affecting detection
    * @return
    *   detection signal strength for this node
    */
  private def nodeSignal(node: Node, malware: Malware): Double =
    node.workload * node.nodeType.detectionCoefficient *
      (DetectionFormula.STEALTH_INVERSION_FACTOR - malware.traits.stealth.value)

  /** Calculates overall detection signal across the entire network. Returns the average detection
    * signal from all nodes.
    *
    * @param topology
    *   the network topology
    * @param malware
    *   the malware to calculate detection for
    * @return
    *   average detection signal (0.0 if no nodes exist)
    */
  def detectionSignal(topology: Topology, malware: Malware): Double =
    if topology.nodes.isEmpty then Bounds.DEFAULT_DETECTION_SIGNAL
    else topology.infectedNodes().map(nodeSignal(_, malware)).sum / topology.nodes.size
