package it.unibo.splague.view.simulation

import it.unibo.splague.model.node.NodeState
import it.unibo.splague.view.form.TopologyForm

import java.awt.{BasicStroke, Color, Graphics2D}
import java.awt.geom.{Ellipse2D, Line2D, Path2D}

/** Draws the workspace canvas: edges (with a directional arrowhead) and nodes (colored by state,
  * highlighted when selected). Pure `Graphics2D` painting — takes the panel's topology/positions/
  * selection as parameters instead of holding any state of its own.
  */
object WorkspaceRenderer:

  def draw(
      g2: Graphics2D,
      topology: TopologyForm,
      nodeViews: collection.Map[String, ViewNode],
      nodeRadius: Double,
      selectedNodeId: Option[String],
      selectedEdgeKey: Option[(String, String)]
  ): Unit =
    drawEdges(g2, topology, nodeViews, nodeRadius, selectedEdgeKey)
    drawNodes(g2, nodeViews, nodeRadius, selectedNodeId)

  private def drawEdges(
      g2: Graphics2D,
      topology: TopologyForm,
      nodeViews: collection.Map[String, ViewNode],
      nodeRadius: Double,
      selectedEdgeKey: Option[(String, String)]
  ): Unit =
    topology.edges.foreach { edge =>
      val selected =
        selectedEdgeKey.contains((edge.from, edge.to))

      g2.setColor(
        if selected then new Color(255, 87, 34)
        else new Color(170, 170, 170)
      )

      g2.setStroke(
        new BasicStroke(
          if selected then 4.0f else 2.0f
        )
      )

      for
        source <- nodeViews.get(edge.from)
        target <- nodeViews.get(edge.to)
      do
        drawEdge(
          g2,
          nodeRadius,
          source.x,
          source.y,
          target.x,
          target.y
        )
    }

  private def drawEdge(
      g2: Graphics2D,
      nodeRadius: Double,
      sourceX: Double,
      sourceY: Double,
      targetX: Double,
      targetY: Double
  ): Unit =
    val dx =
      targetX - sourceX

    val dy =
      targetY - sourceY

    val angle =
      math.atan2(dy, dx)

    val endX =
      targetX -
        math.cos(angle) *
        (nodeRadius + 6.0)

    val endY =
      targetY -
        math.sin(angle) *
        (nodeRadius + 6.0)

    g2.draw(
      new Line2D.Double(
        sourceX,
        sourceY,
        endX,
        endY
      )
    )

    drawArrowHead(g2, endX, endY, angle)

  /** Draws a filled triangular arrowhead pointing along `angle`, tip at `(tipX, tipY)`. Used to
    * show the direction of a connection, since edges are directed.
    */
  private def drawArrowHead(
      g2: Graphics2D,
      tipX: Double,
      tipY: Double,
      angle: Double
  ): Unit =
    val length = 10.0
    val spread = math.toRadians(25.0)

    val leftX = tipX - length * math.cos(angle - spread)
    val leftY = tipY - length * math.sin(angle - spread)

    val rightX = tipX - length * math.cos(angle + spread)
    val rightY = tipY - length * math.sin(angle + spread)

    val arrowHead = new Path2D.Double()
    arrowHead.moveTo(tipX, tipY)
    arrowHead.lineTo(leftX, leftY)
    arrowHead.lineTo(rightX, rightY)
    arrowHead.closePath()

    g2.fill(arrowHead)

  private def colorFor(state: NodeState): Color =
    state match
      case NodeState.Healthy     => new Color(15, 157, 88)
      case NodeState.Infected    => new Color(219, 68, 55)
      case NodeState.Quarantined => new Color(244, 160, 0)
      case NodeState.Immune      => new Color(66, 133, 244)
      case NodeState.Destroyed   => new Color(95, 99, 104)

  private def drawNodes(
      g2: Graphics2D,
      nodeViews: collection.Map[String, ViewNode],
      nodeRadius: Double,
      selectedNodeId: Option[String]
  ): Unit =
    nodeViews.foreach { case (id, nodeView) =>
      val selected =
        selectedNodeId.contains(id)

      val shape =
        new Ellipse2D.Double(
          nodeView.x - nodeRadius,
          nodeView.y - nodeRadius,
          nodeRadius * 2,
          nodeRadius * 2
        )

      g2.setColor(
        colorFor(nodeView.form.state)
      )

      g2.fill(shape)

      g2.setColor(
        if selected then Color.BLACK
        else Color.WHITE
      )

      g2.setStroke(
        new BasicStroke(
          if selected then 3.0f else 2.0f
        )
      )

      g2.draw(shape)

      g2.drawString(
        nodeView.form.id,
        (nodeView.x + nodeRadius + 4).toFloat,
        (nodeView.y + 4).toFloat
      )
    }
