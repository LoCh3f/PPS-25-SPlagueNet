package it.unibo.splague.update

import it.unibo.splague.model.malware.Malware
import it.unibo.splague.model.node.{Node, Topology}

object AwarenessRules:

  private def nodeSignal(node: Node, malware: Malware): Double =
    node.workload * node.nodeType.detectionCoefficient * (1 - malware.traits.stealth.value)

  def detectionSignal(topology: Topology, malware: Malware): Double =
    if topology.nodes.isEmpty then 0.0
    else topology.nodes.values.map(nodeSignal(_, malware)).sum / topology.nodes.size
