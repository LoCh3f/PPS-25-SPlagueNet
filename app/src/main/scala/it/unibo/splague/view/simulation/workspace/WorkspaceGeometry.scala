package it.unibo.splague.view.simulation.workspace

import it.unibo.splague.view.form.EdgeForm

import java.awt.geom.Line2D

/** Screen<->model coordinate conversion and hit-testing for the workspace canvas. Every method
  * takes the panel's current viewport/positions/topology as plain parameters instead of holding any
  * state of its own, so it can be tested without a `Graphics2D` or a live `JPanel`.
  */
object WorkspaceGeometry:

  def screenToModel(
      x: Int,
      y: Int,
      offsetX: Int,
      offsetY: Int,
      zoom: Double
  ): (Double, Double) =
    ((x - offsetX) / zoom, (y - offsetY) / zoom)

  def pickNode(
      x: Double,
      y: Double,
      nodeViews: collection.Map[String, ViewNode],
      nodeRadius: Double
  ): Option[String] =
    nodeViews.collectFirst {
      case (id, nodeView) if math.hypot(x - nodeView.x, y - nodeView.y) <= nodeRadius => id
    }

  def pickEdge(
      x: Double,
      y: Double,
      edges: Vector[EdgeForm],
      nodeViews: collection.Map[String, ViewNode],
      tolerance: Double = 6.0
  ): Option[(String, String)] =
    edges.collectFirst {
      case edge
          if nodeViews
            .get(edge.from)
            .zip(nodeViews.get(edge.to))
            .exists { case (source, target) =>
              new Line2D.Double(
                source.x,
                source.y,
                target.x,
                target.y
              ).ptSegDist(x, y) <= tolerance
            } =>
        (edge.from, edge.to)
    }

  def connectionExists(
      edges: Vector[EdgeForm],
      from: String,
      to: String
  ): Boolean =
    edges.exists { edge =>
      (edge.from == from && edge.to == to) ||
      (edge.from == to && edge.to == from)
    }
