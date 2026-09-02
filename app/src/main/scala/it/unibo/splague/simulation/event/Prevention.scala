package it.unibo.splague.simulation.event

import it.unibo.splague.model.countermeasures.Countermeasures.{DefenseBoost, Patch}
import it.unibo.splague.model.node.Topology
import it.unibo.splague.simulation.Scenario
import it.unibo.splague.simulation.event.SimulationEvents.{Event, TopologyUpdateMixin}
import it.unibo.splague.update.DefenseRules

object Prevention:
  object Prevention extends Event with TopologyUpdateMixin:
    override def apply(scenario: Scenario): Scenario =
      val countermeasureConfig = scenario.countermeasureConfig
      var nodes = scenario.topology.nodes

      // Increase Defense
      if countermeasureConfig.activeCountermeasures.contains(DefenseBoost) then
        nodes = scenario.topology.healthyNodes().foldLeft(nodes) { (acc, n) =>
          acc.updated(n.nodeId.value, DefenseRules.boostDefense(n))
        }

      // Increase Patch
      if countermeasureConfig.activeCountermeasures.contains(Patch) then
        nodes = scenario.topology.healthyNodes().foldLeft(nodes) { (acc, n) =>
          acc.updated(n.nodeId.value, DefenseRules.boostPatch(n))
        }

      scenario.copy(topology = scenario.topology.copy(nodes = nodes))
