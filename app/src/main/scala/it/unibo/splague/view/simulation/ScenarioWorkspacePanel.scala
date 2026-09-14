package it.unibo.splague.view.simulation

import it.unibo.splague.model.connection.Connection.Channel
import it.unibo.splague.model.connection.Connection.ChannelType
import it.unibo.splague.model.node.Node
import it.unibo.splague.update.Msg
import it.unibo.splague.view.form.{ChannelForm, EdgeForm, NodeForm, TopologyForm}

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
import javax.swing.JPanel
import javax.swing.SwingUtilities

import scala.collection.mutable

final case class ViewNode(
    form: NodeForm,
    var x: Double,
    var y: Double
)

/** Responsible for: node screen positions, zoom, pan, node/edge selection, node dragging,
  * mouse/keyboard/wheel handling, opening the editor dialogs, dispatching the already existing
  * messages, and re-syncing state when a new [[TopologyForm]] arrives.
  *
  * Rendering is delegated to [[WorkspaceRenderer]] and coordinate conversion/hit-testing to
  * [[WorkspaceGeometry]]; detailed dialog field logic lives in [[NodeEditorDialog]] and
  * [[EdgeEditorDialog]].
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
            WorkspaceGeometry.screenToModel(
              event.getX,
              event.getY,
              offsetX,
              offsetY,
              zoom
            )

          val selectedNode =
            WorkspaceGeometry.pickNode(
              modelX,
              modelY,
              nodeViews,
              nodeRadius
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
              selectedEdgeKey = WorkspaceGeometry.pickEdge(
                modelX,
                modelY,
                topology.edges,
                nodeViews
              )

              panning = true

          repaint()

        override def mouseClicked(
            event: MouseEvent
        ): Unit =
          if SwingUtilities.isRightMouseButton(event) then
            val (modelX, modelY) =
              WorkspaceGeometry.screenToModel(
                event.getX,
                event.getY,
                offsetX,
                offsetY,
                zoom
              )

            WorkspaceGeometry.pickNode(modelX, modelY, nodeViews, nodeRadius) match
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
                WorkspaceGeometry.pickEdge(modelX, modelY, topology.edges, nodeViews).foreach {
                  key =>
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
              WorkspaceGeometry.screenToModel(
                event.getX,
                event.getY,
                offsetX,
                offsetY,
                zoom
              )

            val targetNode =
              WorkspaceGeometry.pickNode(modelX, modelY, nodeViews, nodeRadius)

            for
              sourceId <- connectionStartNodeId
              targetId <- targetNode
              if sourceId != targetId
              if !WorkspaceGeometry.connectionExists(topology.edges, sourceId, targetId)
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

    WorkspaceRenderer.draw(
      g2,
      topology,
      nodeViews,
      nodeRadius,
      selectedNodeId,
      selectedEdgeKey
    )

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
