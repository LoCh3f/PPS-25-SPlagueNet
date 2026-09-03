package it.unibo.splague.update

import it.unibo.splague.model.Probability
import it.unibo.splague.model.countermeasures.{CountermeasureConfig, Countermeasures}
import it.unibo.splague.model.node.Node

object DefenseRules:

  def boostDefense(node: Node, config: CountermeasureConfig): Node =
    node.copy(defenseLevel = math.min(1.0, node.defenseLevel + config.defenseBoostAmount))

  def boostPatch(node: Node, config: CountermeasureConfig): Node =
    node.copy(patchLevel = math.min(1.0, node.patchLevel + config.patchBoostAmount))

  /** @param node
    *   to try to cure
    * @return
    *   Probability of the node being cured
    *
    * We want unpatched nodes to still have a tiny baseline chance of natural recovery (0.1), while
    * fully patched nodes approach certainty (1.0), scaling linearly from a base value works well.
    */
  def cureProbability(node: Node, config: CountermeasureConfig): Probability =
    if Countermeasures.Patch.isApplicableTo(node.nodeType) then
      Probability.clamped(
        config.patchCureProbability + (1.0 - config.patchCureProbability) * node.patchLevel
      )
    else Probability.clamped(0.0)

  def solveCure(p: Probability, roll: Double): Boolean = roll < p.value
