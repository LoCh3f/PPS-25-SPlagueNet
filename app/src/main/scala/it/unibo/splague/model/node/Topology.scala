package it.unibo.splague.model.node

import it.unibo.splague.model.connection.Connection.Edge

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

    def degree(node: Node): Int =
      neighbors(node).size

    def hub(node: Node): Option[Node] =
      topology.nodes.values.maxByOption(n => degree(n))
