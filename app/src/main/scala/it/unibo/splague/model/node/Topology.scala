package it.unibo.splague.model.node

import it.unibo.splague.model.connection.Connection.Edge

import scala.collection.mutable

@SerialVersionUID(1L)
case class Topology(
    nodes: Map[String, Node],
    edges: Set[Edge]
)

object Topology:
  extension (topology: Topology)
    def edgesOf(node: Node): Set[Edge] =
      topology.edges
        .filter(edge => edge.source.nodeId == node.nodeId || edge.target.nodeId == node.nodeId)
        .map(edge => edge.copy())

    def neighbors(node: Node): Set[Node] =
      topology.edges.flatMap { edge =>
        if edge.source.nodeId == node.nodeId then Set(edge.target)
        else if edge.target.nodeId == node.nodeId then Set(edge.source)
        else Set.empty
      }

    def reachableFrom(node: Node): Set[Node] =
      val initialNeighbors: Set[Node] = neighbors(node)
      val queue = mutable.Queue[Node]()
      val visited = mutable.Set[Node](node)

      queue.enqueueAll(initialNeighbors)
      visited.addAll(initialNeighbors)

      while queue.nonEmpty do
        val curr = queue.dequeue()

        for nextNeighbor <- neighbors(curr) do
          if !visited.contains(nextNeighbor) then
            visited.add(nextNeighbor)
            queue.enqueue(nextNeighbor)

      visited.toSet - node

    def degree(node: Node): Int =
      neighbors(node).size

    def hub(node: Node): Option[Node] =
      topology.nodes.values.maxByOption(n => degree(n))

    def healthyNodes(): Set[Node] =
      topology.nodes.values.filter(node => node.state == NodeState.Healthy).toSet

    def infectedNodes(): Set[Node] =
      topology.nodes.values.filter(node => node.state == NodeState.Infected).toSet

    def quarantinedNodes(): Set[Node] =
      topology.nodes.values.filter(node => node.state == NodeState.Quarantined).toSet

    def destroyedNodes(): Set[Node] =
      topology.nodes.values.filter(node => node.state == NodeState.Destroyed).toSet
