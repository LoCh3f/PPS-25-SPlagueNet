package it.unibo.splague.update.simulation.event

import it.unibo.splague.model.Scenario
import it.unibo.splague.model.countermeasures.Countermeasures.Patch
import it.unibo.splague.model.node.NodeState
import it.unibo.splague.model.node.Topology.infectedNodes
import SimulationEvents.{Event, TopologyUpdateMixin}
import it.unibo.splague.update.simulation.event.rules.DefenseRules

import scala.util.Random

object Cure:

  private val recoveryRate: Double = 0.05

  object CureEvent extends Event with TopologyUpdateMixin:
    override def apply(scenario: Scenario): Scenario =
      val countermeasureConfig = scenario.countermeasureConfig

      if !countermeasureConfig.activeCountermeasures.contains(Patch) then return scenario

      val rng = new Random(scenario.seed + scenario.tick + 1)
      val targets = scenario.topology.infectedNodes() ++ scenario.topology.quarantinedNodes()

      val updatedTopology = targets.foldLeft(scenario.topology) { (currentTopology, node) =>
        val curato = DefenseRules.resolveCure(
          DefenseRules.cureProbability(node, countermeasureConfig),
          rng.nextDouble()
        )

        if curato then
          updateNode(currentTopology, node.nodeId) { currentNode =>
            currentNode.copy(state = NodeState.Immune)
          }
        else currentTopology
      }

      scenario.copy(topology = updatedTopology)

  object LowerWorkloadEvent extends Event with TopologyUpdateMixin:
    override def apply(scenario: Scenario): Scenario =
      val immuneNodes = scenario.topology.nodes.values.filter(_.state == NodeState.Immune)

      val updatedTopology = immuneNodes.foldLeft(scenario.topology) { (currentTopology, node) =>
        updateNode(currentTopology, node.nodeId) { currentNode =>
          val baseline = scenario.baselineWorkload.getOrElse(node, 0.0)

          val newWorkload = math.max(baseline, currentNode.workload - recoveryRate)
          currentNode.copy(workload = newWorkload)
        }
      }

      scenario.copy(topology = updatedTopology)
