package it.unibo.splague.update.simulation.event.rules

import it.unibo.splague.model.Probability
import it.unibo.splague.model.connection.Connection.Edge
import it.unibo.splague.model.malware.Malware
import it.unibo.splague.model.node.Node

object ContagionRules:

  private type Modifier = (Probability, Malware, Node, Edge) => Probability

  private def infectionBase(malware: Malware, node: Node, edge: Edge): Probability =
    malware.traits.infectivity

  private def withDefense: Modifier =
    (base, _, node, edge) => Probability.clamped(base.value * (1 - node.defenseLevel))

  private def withPatch: Modifier =
    (base, _, node, edge) => Probability.clamped(base.value * (1 - node.patchLevel))

  private def withStructuralVulnerability: Modifier =
    (base, _, node, edge) => Probability.clamped(base.value * node.nodeType.structuralVulnerability)

  private def withPacketLoss: Modifier =
    (base, _, node, edge) => Probability.clamped(base.value * (1 - edge.channel.packetLoss.value))

  private val infectionPipeline: Seq[Modifier] = Seq(
    withDefense,
    withPatch,
    withStructuralVulnerability,
    withPacketLoss
  )

  def infectionProbability(malware: Malware, target: Node, edge: Edge): Probability =
    infectionPipeline.foldLeft(infectionBase(malware, target, edge)) { (acc, modifier) =>
      modifier(acc, malware, target, edge)
    }

  private def resolveEvent(probability: Probability, roll: Double): Boolean =
    roll < probability.value

  def resolveInfection(malware: Malware, target: Node, edge: Edge, roll: Double): Boolean =
    resolveEvent(infectionProbability(malware, target, edge), roll)
