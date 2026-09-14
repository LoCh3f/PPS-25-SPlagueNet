package it.unibo.splague.view.form

import it.unibo.splague.model.connection.Connection.Edge
import it.unibo.splague.model.node.{Node, Topology}

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

      nodesMap =
        nodes.map(node => node.nodeId.value -> node).toMap

      edges <- foldEither(Vector.empty[Edge], form.edges) { (acc, edgeForm) =>
        EdgeForm.toDomain(edgeForm, nodesMap).map(acc :+ _)
      }
    yield Topology(
      nodes = nodesMap,
      edges = edges.toSet
    )

  private def checkDuplicateNodeIds(
      nodes: Vector[Node]
  ): Either[String, Unit] =
    val duplicated =
      nodes
        .map(_.nodeId.value)
        .groupBy(identity)
        .collect {
          case (id, occurrences) if occurrences.size > 1 => id
        }

    if duplicated.nonEmpty then Left(s"Duplicated node IDs: ${duplicated.mkString(", ")}")
    else Right(())

  private def foldEither[A, B](
      initial: Vector[A],
      items: Vector[B]
  )(
      f: (Vector[A], B) => Either[String, Vector[A]]
  ): Either[String, Vector[A]] =
    items.foldLeft(
      Right(initial): Either[String, Vector[A]]
    ) { (result, item) =>
      result.flatMap(acc => f(acc, item))
    }
