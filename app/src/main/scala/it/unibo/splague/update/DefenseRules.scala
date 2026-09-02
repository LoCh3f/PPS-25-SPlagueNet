package it.unibo.splague.update

import it.unibo.splague.model.node.Node

object DefenseRules:
  private val DefenseBoostAmount: Double = 0.05
  private val PatchBoostAmount: Double = 0.05
  private val PatchCureProbability: Double = 0.5

  def boostDefense(node: Node): Node =
    node.copy(defenseLevel = math.min(1.0, node.defenseLevel + DefenseBoostAmount))

  def boostPatch(node: Node): Node =
    node.copy(patchLevel = math.min(1.0, node.patchLevel + PatchBoostAmount))
