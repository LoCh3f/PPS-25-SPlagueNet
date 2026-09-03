package it.unibo.splague.simulation.event

import it.unibo.splague.model.countermeasures.Countermeasures.Patch
import it.unibo.splague.model.node.NodeState
import it.unibo.splague.model.node.Topology.infectedNodes
import it.unibo.splague.simulation.Scenario
import it.unibo.splague.simulation.event.SimulationEvents.{Event, TopologyUpdateMixin}
import it.unibo.splague.update.DefenseRules

import scala.util.Random

object Cure:
  object Cure extends Event with TopologyUpdateMixin:
    override def apply(scenario: Scenario): Scenario =
      val countermeasureConfig = scenario.countermeasureConfig
      var nodes = scenario.topology.nodes

      if !countermeasureConfig.activeCountermeasures.contains(Patch) then scenario
      else
        val rng = new Random(scenario.seed + scenario.tick + 1)

        // Try to cure every infected or quarantined node
        val targets = scenario.topology.infectedNodes() ++ scenario.topology.quarantinedNodes()

        val newNodes = targets.foldLeft(scenario.topology.nodes) { (acc, node) =>
          val curato = DefenseRules.resolveCure(
            DefenseRules.cureProbability(node, countermeasureConfig),
            rng.nextDouble()
          )
          if curato then acc.updated(node.nodeId.value, node.copy(state = NodeState.Immune))
          else acc
        }

        scenario.copy(topology = scenario.topology.copy(nodes = newNodes))
