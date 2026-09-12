package it.unibo.splague.update.simulation.event

import it.unibo.splague.model.Scenario
import it.unibo.splague.model.node.NodeId.NodeId
import it.unibo.splague.model.node.{Node, Topology}

object SimulationEvents:

  trait Event:
    def apply(scenario: Scenario): Scenario

  trait EventSelector:
    def nextEvent(scenario: Scenario): Event

  trait TopologyUpdateMixin:
    self: Event =>

    protected def updateNode(
        topology: Topology,
        nodeId: NodeId
    )(f: Node => Node): Topology =
      topology.nodes.get(nodeId.value) match
        case Some(node) =>
          val updated = f(node)
          val newNodes = topology.nodes.updated(nodeId.value, updated)
          topology.copy(nodes = newNodes)
        case None =>
          topology
