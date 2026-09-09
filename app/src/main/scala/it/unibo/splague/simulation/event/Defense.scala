package it.unibo.splague.simulation.event

import it.unibo.splague.model.countermeasures.Countermeasures.{Firewall, Isolation}
import it.unibo.splague.model.node.NodeId.NodeId
import it.unibo.splague.model.node.{Node, NodeState, Topology}
import it.unibo.splague.simulation.Scenario
import it.unibo.splague.simulation.event.SimulationEvents.{Event, TopologyUpdateMixin}
import it.unibo.splague.update.FirewallPolicy

object Defense:
  private def cutEdgesOf(topology: Topology, nodes: Set[Node]): Topology =
    topology.copy(edges =
      topology.edges.filterNot(e => nodes.contains(e.source) || nodes.contains(e.target))
    )

  private def cutEdgesWithFirewall(topology: Topology, firewallPolicy: FirewallPolicy): Topology =
    topology.copy(edges =
      topology.edges.filterNot(e => FirewallPolicy.isBlocked(e, firewallPolicy))
    )

  object IsolationEvent extends Event with TopologyUpdateMixin:
    override def apply(scenario: Scenario): Scenario =
      val config = scenario.countermeasureConfig

      if config.activeCountermeasures.contains(Isolation) then scenario
      else
        val criteria = config.isolationCriteria
        val targets = scenario.topology.infectedNodes().filter(criteria.matches).toSet

        if targets.isEmpty then scenario
        else
          val updatedActive = config.activeCountermeasures + Isolation
          val updatedNodes = targets.foldLeft(scenario.topology.nodes) { (acc, node) =>
            acc.updated(node.nodeId.value, node.copy(state = NodeState.Quarantined))
          }

          // Remove connections
          val topologyWithoutEdges =
            cutEdgesOf(scenario.topology.copy(nodes = updatedNodes), targets)
          scenario.copy(
            topology = topologyWithoutEdges,
            countermeasureConfig = config.copy(activeCountermeasures = updatedActive)
          )

  object FirewallEvent extends Event:
    override def apply(scenario: Scenario): Scenario =
      val config = scenario.countermeasureConfig

      if config.activeCountermeasures.contains(Firewall) then scenario
      else
        val updatedActive = config.activeCountermeasures + Firewall
        val updatedPolicy = config.firewallPolicy.merge(FirewallPolicy.defaultPolicy)

        val topologyWithoutEdges = cutEdgesWithFirewall(scenario.topology, updatedPolicy)
        scenario.copy(
          topology = topologyWithoutEdges,
          countermeasureConfig = config.copy(
            activeCountermeasures = updatedActive,
            firewallPolicy = updatedPolicy
          )
        )
