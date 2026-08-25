package it.unibo.splague.model.node

import it.unibo.splague.model.connection.Connection.Edge

@SerialVersionUID(1L)
case class Topology(
    @transient nodes: Map[String, Node],
    @transient edges: Set[Edge]
)

object Topology:
  extension (topology: Topology)
    def neighbors(node: Node): Set[Node] =
      topology.edges.flatMap { edge =>
        if edge.source == node then Set(edge.target)
        else if edge.target == node then Set(edge.source)
        else Set.empty
      }
