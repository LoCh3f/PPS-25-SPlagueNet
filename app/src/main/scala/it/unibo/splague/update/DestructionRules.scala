package it.unibo.splague.update

import it.unibo.splague.model.Probability
import it.unibo.splague.model.malware.{Malware, PayloadSeverityLevel}
import it.unibo.splague.model.node.Node

object DestructionRules:

  /** @param node
    *   target infected
    * @param malware
    *   that infected the target node
    * @return
    *   the new workload level of the target node The formula takes into account the
    *   PayloadSeverityLevel and the footprint of the Malware.
    */
  def increaseWorkload(node: Node, malware: Malware): Double =
    val severityFactor = malware.traits.payloadSeverity match
      case PayloadSeverityLevel.Low    => 0.1
      case PayloadSeverityLevel.Medium => 0.2
      case PayloadSeverityLevel.High   => 0.4

    val increment = malware.traits.footprint.value * severityFactor
    math.min(1.0, node.workload + increment)

  /** @param node
    *   target infected node
    * @return
    *   Probability in range (0,1) that the node will get destroyed. It is based on the node's
    *   workload level.
    */
  def destructionProbability(node: Node): Probability =
    Probability.clamped(node.workload)

  def resolveDestruction(node: Node, roll: Double): Boolean =
    val p = destructionProbability(node)
    roll < p.value
