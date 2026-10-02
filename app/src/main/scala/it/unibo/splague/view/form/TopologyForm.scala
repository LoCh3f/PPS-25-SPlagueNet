package it.unibo.splague.view.form

import it.unibo.splague.model.connection.Connection.Edge
import it.unibo.splague.model.node.{Node, Topology}
import it.unibo.splague.view.form.FormParsing.*

final case class TopologyForm(
    nodes: Vector[NodeForm],
    edges: Vector[EdgeForm]
)

object TopologyForm:

  def fromTopology(topology: Topology): TopologyForm =
    TopologyForm(
      nodes = topology.nodes.values.toVector.map(NodeForm.fromNode),
      edges = topology.edges.toVector.map(EdgeForm.fromEdge)
    )

  def toDomain(form: TopologyForm): Either[String, Topology] =
    for
      nodes <- foldEither(Vector.empty[Node], form.nodes) { (acc, nodeForm) =>
        NodeForm.toDomain(nodeForm).map(acc :+ _)
      }
      _ <- checkDuplicateNodeIds(nodes)
      nodesMap = nodes.map(node => node.nodeId.value -> node).toMap
      edges <- foldEither(Vector.empty[Edge], form.edges) { (acc, edgeForm) =>
        EdgeForm.toDomain(edgeForm, nodesMap).map(acc :+ _)
      }
    yield Topology(
      nodes = nodesMap,
      edges = edges.toSet
    )
