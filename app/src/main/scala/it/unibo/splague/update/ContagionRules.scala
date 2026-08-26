package it.unibo.splague.update

import it.unibo.splague.model.malware.Malware
import it.unibo.splague.model.{Node, NodeType, Probability}
import it.unibo.splague.model.Probability

object ContagionRules:
  /** Current milestone: infectivity alone, nothing else wired in yet. */
  def infectionProbability(malware: Malware): Probability =
    malware.traits.infectivity

  def withStructuralVulnerability(base: Probability, nodeType: NodeType): Probability =
    Probability.clamped(base.value * nodeType.structuralVulnerability)

  def withDefense(base: Probability, node: Node): Probability =
    Probability.clamped(base.value * (1 - node.defenseLevel))

  def withPatch(base: Probability, node: Node): Probability =
    Probability.clamped(base.value * (1 - node.patchLevel))

  def compute(malware: Malware, target: Node): Probability =
    val base = infectionProbability(malware)
    val v1 = withStructuralVulnerability(base, target.nodeType)
    withDefense(v1, target)
