package it.unibo.splague.simulation.event

import it.unibo.splague.model.node.NodeState
import it.unibo.splague.simulation.Scenario
import it.unibo.splague.simulation.event.SimulationEvents.{Event, TopologyUpdateMixin}
import it.unibo.splague.update.DestructionRules

import scala.util.Random

object Destroy:
  object DestroyEvent extends Event with TopologyUpdateMixin:
    override def apply(scenario: Scenario): Scenario =

      val infectedNodes = scenario.topology.infectedNodes()

      val rng = new Random(scenario.seed + scenario.tick + 1)

      val updatedNodes = infectedNodes.foldLeft(scenario.topology.nodes) { (acc, node) =>
        val destroyed = DestructionRules.resolveDestruction(
          node,
          rng.nextDouble()
        )

        if destroyed then acc.updated(node.nodeId.value, node.copy(state = NodeState.Destroyed))
        else acc
      }

      scenario.copy(topology = scenario.topology.copy(nodes = updatedNodes))
