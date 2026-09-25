package it.unibo.splague.update.simulation.event

import it.unibo.splague.model.Scenario
import it.unibo.splague.model.node.NodeState
import SimulationEvents.{Event, TopologyUpdateMixin}
import it.unibo.splague.update.simulation.event.rules.DestructionRules

import scala.util.Random

object Destroy:
  object IncreaseWorkloadEvent extends Event with TopologyUpdateMixin:
    override def apply(scenario: Scenario): Scenario =

      val updatedTopology =
        scenario.topology.infectedNodes().foldLeft(scenario.topology) { (currentTopology, node) =>
          updateNode(currentTopology, node.nodeId) { currentNode =>
            val updatedWorkload = DestructionRules.increaseWorkload(
              currentNode,
              scenario.virus
            )

            currentNode.copy(workload = updatedWorkload)
          }
        }

      scenario.copy(topology = updatedTopology)

  object DestroyEvent extends Event with TopologyUpdateMixin:
    override def apply(scenario: Scenario): Scenario =
      val infectedNodes = scenario.topology.infectedNodes()
      val rng = new Random(scenario.seed + scenario.tick + 1)

      val updatedTopology = infectedNodes.foldLeft(scenario.topology) { (currentTopology, node) =>
        val destroyed = DestructionRules.resolveDestruction(
          node,
          rng.nextDouble()
        )

        if destroyed then
          updateNode(currentTopology, node.nodeId) { currentNode =>
            currentNode.copy(state = NodeState.Destroyed)
          }
        else currentTopology
      }

      scenario.copy(topology = updatedTopology)
