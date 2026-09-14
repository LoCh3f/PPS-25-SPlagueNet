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
      var nodes = scenario.topology.nodes

      // Increase Defense
      if countermeasureConfig.activeCountermeasures.contains(DefenseBoost) then
        nodes = scenario.topology.healthyNodes().foldLeft(nodes) { (acc, n) =>
          acc.updated(n.nodeId.value, DefenseRules.boostDefense(n, countermeasureConfig))
        }

      scenario.copy(topology = scenario.topology.copy(nodes = nodes))

  object PatchBoostEvent extends Event with TopologyUpdateMixin:
    override def apply(scenario: Scenario): Scenario =
      val countermeasureConfig = scenario.countermeasureConfig
      var nodes = scenario.topology.nodes

      // Increase Patch
      if countermeasureConfig.activeCountermeasures.contains(Patch) then
        nodes = scenario.topology.healthyNodes().foldLeft(nodes) { (acc, n) =>
          acc.updated(n.nodeId.value, DefenseRules.boostPatch(n, countermeasureConfig))
        }

      scenario.copy(topology = scenario.topology.copy(nodes = nodes))
