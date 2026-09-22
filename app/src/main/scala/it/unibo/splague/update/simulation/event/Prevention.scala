package it.unibo.splague.update.simulation.event

import it.unibo.splague.model.Scenario
import it.unibo.splague.model.countermeasures.Countermeasures.{DefenseBoost, Patch}
import it.unibo.splague.model.node.Topology
import SimulationEvents.{Event, TopologyUpdateMixin}
import it.unibo.splague.update.simulation.event.rules.DefenseRules

object Prevention:
  object DefenseBoostEvent extends Event with TopologyUpdateMixin:
    override def apply(scenario: Scenario): Scenario =
      val countermeasureConfig = scenario.countermeasureConfig

      // Increase Defense
      if countermeasureConfig.activeCountermeasures.contains(DefenseBoost) then
        val updatedTopology =
          scenario.topology.healthyNodes().foldLeft(scenario.topology) { (currentTopology, node) =>
            updateNode(currentTopology, node.nodeId) { currentNode =>
              DefenseRules.boostDefense(currentNode, countermeasureConfig)
            }
          }
        scenario.copy(topology = updatedTopology)
      else scenario

  object PatchBoostEvent extends Event with TopologyUpdateMixin:
    override def apply(scenario: Scenario): Scenario =
      val countermeasureConfig = scenario.countermeasureConfig

      // Increase Patch
      if countermeasureConfig.activeCountermeasures.contains(Patch) then
        val updatedTopology =
          scenario.topology.healthyNodes().foldLeft(scenario.topology) { (currentTopology, node) =>
            updateNode(currentTopology, node.nodeId) { currentNode =>
              DefenseRules.boostPatch(currentNode, countermeasureConfig)
            }
          }
        scenario.copy(topology = updatedTopology)
      else scenario
