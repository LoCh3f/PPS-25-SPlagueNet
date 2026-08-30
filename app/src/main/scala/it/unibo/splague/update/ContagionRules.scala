package it.unibo.splague.update

import it.unibo.splague.model.malware.Malware
import it.unibo.splague.model.node.Node
import it.unibo.splague.model.Probability

object ContagionRules:

  private type Modifier = (Probability, Malware, Node) => Probability

  private def infectionBase(malware: Malware, node: Node): Probability =
    malware.traits.infectivity

  private def withDefense: Modifier =
    (base, _, node) => Probability.clamped(base.value * (1 - node.defenseLevel))

  private def withPatch: Modifier =
    (base, _, node) => Probability.clamped(base.value * (1 - node.patchLevel))

  private def withStructuralVulnerability: Modifier =
    (base, _, node) => Probability.clamped(base.value * node.nodeType.structuralVulnerability)

  // TODO: withPropagationFactor was removed because ChannelType has no propagationFactor field yet
  // (see Connection module). Once it's added, reintroduce `channel: Channel` into Modifier,
  // infectionBase, infectionProbability and resolveInfection, and add a withPropagationFactor
  // modifier back into this pipeline.
  private val infectionPipeline: Seq[Modifier] = Seq(
    withDefense,
    withPatch,
    withStructuralVulnerability
  )

  def infectionProbability(malware: Malware, target: Node): Probability =
    infectionPipeline.foldLeft(infectionBase(malware, target)) { (acc, modifier) =>
      modifier(acc, malware, target)
    }

  private def resolveEvent(probability: Probability, roll: Double): Boolean =
    roll < probability.value

  def resolveInfection(malware: Malware, target: Node, roll: Double): Boolean =
    resolveEvent(infectionProbability(malware, target), roll)
