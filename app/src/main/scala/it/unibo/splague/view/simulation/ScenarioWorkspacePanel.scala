package it.unibo.splague.view.simulation

import it.unibo.splague.model.connection.Connection.Channel
import it.unibo.splague.model.connection.Connection.ChannelType
import it.unibo.splague.model.node.{Node, NodeState}
import it.unibo.splague.update.Msg
import it.unibo.splague.view.form.{ChannelForm, EdgeForm, NodeForm, TopologyForm}

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import javax.swing.JPanel
import javax.swing.SwingUtilities

import scala.collection.mutable

final case class ViewNode(
    form: NodeForm,
    var x: Double,
    var y: Double
)

/** Responsible only for: graph rendering, node screen positions, zoom, pan, node/edge selection,
  * hit testing, node dragging, mouse/keyboard/wheel handling, opening the editor dialogs,
  * dispatching the already existing messages, and re-syncing graphics when a new [[TopologyForm]]
  * arrives.
  *
  * Detailed dialog field logic lives in [[NodeEditorDialog]] and [[EdgeEditorDialog]].
  *
  * `EdgeForm` has no `id` field: edge identity is the `(from, to)` pair, so
  * selection/hit-testing/deletion are keyed on that pair instead of a synthetic id.
  */
final class ScenarioWorkspacePanel(
    initialTopology: TopologyForm,
    dispatch: Msg => Unit
) extends JPanel:

  private val nodeRadius =
    18.0

  private var topology =
    initialTopology

  private val nodeViews =
    mutable.LinkedHashMap.empty[String, ViewNode]

  private var selectedNodeId: Option[String] =
    None

  private var selectedEdgeKey: Option[(String, String)] =
    None

  private var dragStartScreenX =
    0

  private var dragStartScreenY =
    0

  private var nodeStartX =
    0.0

  private var nodeStartY =
    0.0

  private var panning =
    false

  private var connectionStartNodeId: Option[String] =
    None

  private var creatingConnection =
    false

  private var zoom =
    1.0

  private var offsetX =
    0

  private var offsetY =
    0

  setBackground(Color.WHITE)
  setFocusable(true)

  rebuildNodeViews()
  installMouseHandler()
  installKeyboardHandler()

  def setTopology(
      newTopology: TopologyForm
  ): Unit =
    topology = newTopology

    rebuildNodeViews()
    repaint()

  def zoomIn(): Unit =
    zoom = math.min(4.0, zoom * 1.1)

    repaint()

  def zoomOut(): Unit =
    zoom = math.max(0.2, zoom / 1.1)

    repaint()

  def pan(
      dx: Int,
      dy: Int
  ): Unit =
    offsetX += dx
    offsetY += dy
    repaint()

  private def installMouseHandler(): Unit =
    val handler =
      new MouseAdapter:

        override def mousePressed(
            event: MouseEvent
        ): Unit =
          requestFocusInWindow()

          val (modelX, modelY) =
            screenToModel(
              event.getX,
              event.getY
            )

          val selectedNode =
            pickNode(
              modelX,
              modelY
            )

          if event.isControlDown && selectedNode.isEmpty then
            NodeEditorDialog.show(
              owner = SwingUtilities.getWindowAncestor(ScenarioWorkspacePanel.this),
              initial = newNodeFormTemplate(),
              screenX = event.getXOnScreen,
              screenY = event.getYOnScreen,
              isNew = true,
              dispatch = dispatch
            )

            return

          selectedNodeId = selectedNode

          selectedEdgeKey = None

          dragStartScreenX = event.getX

          dragStartScreenY = event.getY

          selectedNode match
            case Some(nodeId) if event.isShiftDown =>
              connectionStartNodeId = Some(nodeId)

              creatingConnection = true

              panning = false

            case Some(nodeId) =>
              nodeViews.get(nodeId).foreach { nodeView =>
                nodeStartX = nodeView.x

                nodeStartY = nodeView.y
              }

              panning = false

            case None =>
              selectedEdgeKey = pickEdge(
                modelX,
                modelY
              )

              panning = true

          repaint()

        override def mouseClicked(
            event: MouseEvent
        ): Unit =
          if SwingUtilities.isRightMouseButton(event) then
            val (modelX, modelY) =
              screenToModel(
                event.getX,
                event.getY
              )

            pickNode(modelX, modelY) match
              case Some(nodeId) =>
                selectedNodeId = Some(nodeId)

                selectedEdgeKey = None

                nodeViews.get(nodeId).foreach { nodeView =>
                  NodeEditorDialog.show(
                    owner = SwingUtilities.getWindowAncestor(ScenarioWorkspacePanel.this),
                    initial = nodeView.form,
                    screenX = event.getXOnScreen,
                    screenY = event.getYOnScreen,
                    isNew = false,
                    dispatch = dispatch
                  )
                }

              case None =>
                pickEdge(modelX, modelY).foreach { key =>
                  selectedNodeId = None

                  selectedEdgeKey = Some(key)

                  findEdge(key).foreach { edge =>
                    EdgeEditorDialog.show(
                      owner = SwingUtilities.getWindowAncestor(ScenarioWorkspacePanel.this),
                      initial = edge,
                      screenX = event.getXOnScreen,
                      screenY = event.getYOnScreen,
                      isNew = false,
                      dispatch = dispatch
                    )
                  }
                }

            repaint()

        override def mouseDragged(
            event: MouseEvent
        ): Unit =
          selectedNodeId match
            case Some(nodeId) if !creatingConnection =>
              nodeViews.get(nodeId).foreach { nodeView =>
                val dx =
                  (event.getX - dragStartScreenX) / zoom

                val dy =
                  (event.getY - dragStartScreenY) / zoom

                nodeView.x = nodeStartX + dx

                nodeView.y = nodeStartY + dy

                repaint()
              }

            case None if panning =>
              val dx =
                event.getX - dragStartScreenX

              val dy =
                event.getY - dragStartScreenY

              pan(dx, dy)

              dragStartScreenX = event.getX

              dragStartScreenY = event.getY

            case _ =>
              ()

        override def mouseReleased(
            event: MouseEvent
        ): Unit =
          if creatingConnection then
            val (modelX, modelY) =
              screenToModel(
                event.getX,
                event.getY
              )

            val targetNode =
              pickNode(modelX, modelY)

            for
              sourceId <- connectionStartNodeId
              targetId <- targetNode
              if sourceId != targetId
              if !connectionExists(sourceId, targetId)
            do
              val edge =
                EdgeForm(
                  from = sourceId,
                  to = targetId,
                  channel = defaultChannelForm(),
                  protocol = None
                )

              EdgeEditorDialog.show(
                owner = SwingUtilities.getWindowAncestor(ScenarioWorkspacePanel.this),
                initial = edge,
                screenX = event.getXOnScreen,
                screenY = event.getYOnScreen,
                isNew = true,
                dispatch = dispatch
              )

          panning = false

          creatingConnection = false

          connectionStartNodeId = None

        override def mouseWheelMoved(
            event: MouseWheelEvent
        ): Unit =
          if event.getPreciseWheelRotation < 0 then zoomIn()
          else zoomOut()

    addMouseListener(handler)
    addMouseMotionListener(handler)
    addMouseWheelListener(handler)

    addFocusListener(
      new FocusAdapter:
        override def focusLost(
            event: FocusEvent
        ): Unit =
          panning = false

          creatingConnection = false

          connectionStartNodeId = None
    )

  private def installKeyboardHandler(): Unit =
    addKeyListener(
      new KeyAdapter:
        override def keyPressed(
            event: KeyEvent
        ): Unit =
          if event.getKeyCode == KeyEvent.VK_DELETE then
            selectedNodeId.foreach { nodeId =>
              dispatch(
                Msg.RemoveNode(nodeId)
              )
            }

            selectedEdgeKey
              .flatMap(findEdge)
              .foreach { edge =>
                dispatch(
                  Msg.RemoveEdge(edge)
                )
              }

            selectedNodeId = None

            selectedEdgeKey = None

            repaint()
    )

  /** Initial values for a brand-new node: derived from the domain's own default constants
    * (`Node.defaultXxx`), since `NodeForm` provides no `empty` /factory of its own. The new node is
    * always Healthy, as required.
    */
  private def newNodeFormTemplate(): NodeForm =
    NodeForm(
      id = "",
      nodeType = Node.defaultNodeType,
      patchLevel = Node.defaultPatchLevel.toString,
      defenseLevel = Node.defaultDefenseLevel.toString,
      state = Node.defaultState,
      workload = Node.defaultWorkload.toString,
      vectors = Node.defaultVectors
    )

  /** Initial channel for a brand-new edge: derived from the domain's own `Channel.default`, since
    * neither `EdgeForm` nor `ChannelForm` provide a default of their own. LAN is used as the
    * initial channel type.
    */
  private def defaultChannelForm(): ChannelForm =
    ChannelForm.fromChannel(
      Channel.default(ChannelType.LAN)
    )

  private def findEdge(
      key: (String, String)
  ): Option[EdgeForm] =
    topology.edges.find { edge =>
      (edge.from, edge.to) == key
    }

  override def paintComponent(
      graphics: Graphics
  ): Unit =
    super.paintComponent(graphics)

    val g2 =
      graphics.asInstanceOf[Graphics2D]

    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON
    )

    g2.translate(
      offsetX,
      offsetY
    )

    g2.scale(
      zoom,
      zoom
    )

    drawEdges(g2)
    drawNodes(g2)

  private def drawEdges(
      g2: Graphics2D
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
          source.x,
          source.y,
          target.x,
          target.y
        )
    }

  private def drawEdge(
      g2: Graphics2D,
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

  private def colorFor(state: NodeState): Color =
    state match
      case NodeState.Healthy     => new Color(15, 157, 88)
      case NodeState.Infected    => new Color(219, 68, 55)
      case NodeState.Quarantined => new Color(244, 160, 0)
      case NodeState.Immune      => new Color(66, 133, 244)
      case NodeState.Destroyed   => new Color(95, 99, 104)

  private def drawNodes(
      g2: Graphics2D
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

  private def rebuildNodeViews(): Unit =
    val previous =
      nodeViews.toMap

    nodeViews.clear()

    topology.nodes
      .sortBy(_.id)
      .zipWithIndex
      .foreach { case (node, index) =>
        val row =
          index / 6

        val column =
          index % 6

        val view =
          previous.get(node.id) match
            case Some(old) =>
              ViewNode(
                form = node,
                x = old.x,
                y = old.y
              )

            case None =>
              ViewNode(
                form = node,
                x = 80.0 + column * 130.0,
                y = 80.0 + row * 130.0
              )

        nodeViews.put(
          node.id,
          view
        )
      }

  private def screenToModel(
      x: Int,
      y: Int
  ): (Double, Double) =
    (
      (x - offsetX) / zoom,
      (y - offsetY) / zoom
    )

  private def pickNode(
      x: Double,
      y: Double
  ): Option[String] =
    nodeViews.collectFirst {
      case (id, nodeView)
          if math.hypot(
            x - nodeView.x,
            y - nodeView.y
          ) <= nodeRadius =>
        id
    }

  private def pickEdge(
      x: Double,
      y: Double
  ): Option[(String, String)] =
    topology.edges.collectFirst {
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
              ).ptSegDist(x, y) <= 6.0
            } =>
        (edge.from, edge.to)
    }

  private def connectionExists(
      from: String,
      to: String
  ): Boolean =
    topology.edges.exists { edge =>
      (edge.from == from && edge.to == to) ||
      (edge.from == to && edge.to == from)
    }
