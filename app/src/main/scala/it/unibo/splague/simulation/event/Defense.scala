package it.unibo.splague.simulation.event

import it.unibo.splague.model.countermeasures.Countermeasures.Isolation
import it.unibo.splague.model.node.NodeId.NodeId
import it.unibo.splague.model.node.{Node, NodeState, Topology}
import it.unibo.splague.simulation.Scenario
import it.unibo.splague.simulation.event.SimulationEvents.{Event, TopologyUpdateMixin}

object Defense:
  private def cutEdgesOf(topology: Topology, nodes: Set[Node]): Topology =
    topology.copy(edges =
      topology.edges.filterNot(e => nodes.contains(e.source) || nodes.contains(e.target))
    )

  object IsolationEvent extends Event with TopologyUpdateMixin:
    override def apply(scenario: Scenario): Scenario =
      val config = scenario.countermeasureConfig

      if !config.activeCountermeasures.contains(Isolation) then scenario
      else
        val criteria = config.isolationCriteria
        val targets = scenario.topology.infectedNodes().filter(criteria.matches).toSet

        if targets.isEmpty then scenario
        else
          val updatedNodes = targets.foldLeft(scenario.topology.nodes) { (acc, node) =>
            acc.updated(node.nodeId.value, node.copy(state = NodeState.Quarantined))
          }

          // Remove connections
          val topologyWithoutEdges =
            cutEdgesOf(scenario.topology.copy(nodes = updatedNodes), targets)
          scenario.copy(topology = topologyWithoutEdges)
