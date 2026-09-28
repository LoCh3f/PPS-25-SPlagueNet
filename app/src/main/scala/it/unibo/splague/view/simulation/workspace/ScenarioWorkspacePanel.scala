package it.unibo.splague.view.simulation.workspace

import it.unibo.splague.model.connection.Connection.Channel
import it.unibo.splague.model.connection.Connection.ChannelType
import it.unibo.splague.model.node.Node
import it.unibo.splague.update.Msg
import it.unibo.splague.view.simulation.dialog.{EdgeEditorDialog, NodeEditorDialog}
import it.unibo.splague.view.form.{ChannelForm, EdgeForm, NodeForm, TopologyForm}

import java.awt.{Color, Graphics2D, RenderingHints}

import scala.collection.mutable
import scala.swing.{Component, Window}
import scala.swing.event.{
  Key,
  KeyPressed,
  MouseClicked,
  MouseDragged,
  MouseEvent as SwingMouseEvent,
  MousePressed,
  MouseReleased,
  MouseWheelMoved
}

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
    owner: Window,
    dispatch: Msg => Unit
) extends Component:

  private val nodeRadius = 18.0

  private var topology = initialTopology

  private val nodeViews = mutable.LinkedHashMap.empty[String, ViewNode]

  private var selectedNodeId: Option[String] = None
  private var selectedEdgeKey: Option[(String, String)] = None

  private var dragStartScreenX = 0
  private var dragStartScreenY = 0
  private var nodeStartX = 0.0
  private var nodeStartY = 0.0

  private var panning = false

  private var connectionStartNodeId: Option[String] = None
  private var creatingConnection = false

  private var zoom = 1.0
  private var offsetX = 0
  private var offsetY = 0

  background = Color.WHITE
  focusable = true

  rebuildNodeViews()
  installMouseHandler()
  installKeyboardHandler()

  def setTopology(newTopology: TopologyForm): Unit =
    topology = newTopology
    rebuildNodeViews()
    repaint()

  def zoomIn(): Unit =
    zoom = math.min(4.0, zoom * 1.1)
    repaint()

  def zoomOut(): Unit =
    zoom = math.max(0.2, zoom / 1.1)
    repaint()

  def pan(dx: Int, dy: Int): Unit =
    offsetX += dx
    offsetY += dy
    repaint()

  private def toScreenPoint(local: java.awt.Point): java.awt.Point =
    val screenPoint = new java.awt.Point(local)
    javax.swing.SwingUtilities.convertPointToScreen(screenPoint, peer)
    screenPoint

  /** Converts a screen-space point to model coordinates and resolves the node (if any) under it in
    * one step, since this pair of operations is needed at every mouse-interaction entry point
    * (press, popup-click, release).
    */
  private def modelPointAndNodeAt(point: java.awt.Point): (Double, Double, Option[String]) =
    val (modelX, modelY) =
      WorkspaceGeometry.screenToModel(point.x, point.y, offsetX, offsetY, zoom)
    val nodeId = WorkspaceGeometry.pickNode(modelX, modelY, nodeViews, nodeRadius)
    (modelX, modelY, nodeId)

  private def installMouseHandler(): Unit =
    listenTo(mouse.clicks, mouse.moves, mouse.wheel)

    reactions += {
      case e @ MousePressed(_, point, modifiers, _, _) =>
        requestFocusInWindow()

        val (modelX, modelY, selectedNode) = modelPointAndNodeAt(point)

        val ctrlDown = (modifiers & Key.Modifier.Control) != 0
        val shiftDown = (modifiers & Key.Modifier.Shift) != 0

        if ctrlDown && selectedNode.isEmpty then
          val screenPoint = toScreenPoint(point)

          NodeEditorDialog.show(
            owner = owner,
            initial = newNodeFormTemplate(),
            screenX = screenPoint.x,
            screenY = screenPoint.y,
            isNew = true,
            dispatch = dispatch
          )
        else
          selectedNodeId = selectedNode
          selectedEdgeKey = None
          dragStartScreenX = point.x
          dragStartScreenY = point.y

          selectedNode match
            case Some(nodeId) if shiftDown =>
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
              selectedEdgeKey =
                WorkspaceGeometry.pickEdge(modelX, modelY, topology.edges, nodeViews)
              panning = true

          repaint()

      case MouseClicked(_, point, _, _, triggersPopup) if triggersPopup =>
        val (modelX, modelY, pickedNode) = modelPointAndNodeAt(point)

        val screenPoint = toScreenPoint(point)

        pickedNode match
          case Some(nodeId) =>
            selectedNodeId = Some(nodeId)
            selectedEdgeKey = None

            nodeViews.get(nodeId).foreach { nodeView =>
              NodeEditorDialog.show(
                owner = owner,
                initial = nodeView.form,
                screenX = screenPoint.x,
                screenY = screenPoint.y,
                isNew = false,
                dispatch = dispatch
              )
            }

          case None =>
            WorkspaceGeometry.pickEdge(modelX, modelY, topology.edges, nodeViews).foreach { key =>
              selectedNodeId = None
              selectedEdgeKey = Some(key)

              findEdge(key).foreach { edge =>
                EdgeEditorDialog.show(
                  owner = owner,
                  initial = edge,
                  screenX = screenPoint.x,
                  screenY = screenPoint.y,
                  isNew = false,
                  dispatch = dispatch
                )
              }
            }

        repaint()

      case MouseDragged(_, point, _) =>
        selectedNodeId match
          case Some(nodeId) if !creatingConnection =>
            nodeViews.get(nodeId).foreach { nodeView =>
              val dx = (point.x - dragStartScreenX) / zoom
              val dy = (point.y - dragStartScreenY) / zoom

              nodeView.x = nodeStartX + dx
              nodeView.y = nodeStartY + dy

              repaint()
            }

          case None if panning =>
            val dx = point.x - dragStartScreenX
            val dy = point.y - dragStartScreenY

            pan(dx, dy)

            dragStartScreenX = point.x
            dragStartScreenY = point.y

          case _ => ()

      case MouseReleased(_, point, _, _, _) =>
        if creatingConnection then
          val (_, _, targetNode) = modelPointAndNodeAt(point)
          val screenPoint = toScreenPoint(point)

          for
            sourceId <- connectionStartNodeId
            targetId <- targetNode
            if sourceId != targetId
            if !WorkspaceGeometry.connectionExists(topology.edges, sourceId, targetId)
          do
            val edge = EdgeForm(
              from = sourceId,
              to = targetId,
              channel = defaultChannelForm(),
              protocol = None
            )

            EdgeEditorDialog.show(
              owner = owner,
              initial = edge,
              screenX = screenPoint.x,
              screenY = screenPoint.y,
              isNew = true,
              dispatch = dispatch
            )

        panning = false
        creatingConnection = false
        connectionStartNodeId = None

      case MouseWheelMoved(_, _, _, rotation) =>
        if rotation < 0 then zoomIn()
        else zoomOut()
    }

    listenTo(this)
    reactions += { case scala.swing.event.FocusLost(_, _, _) =>
      panning = false
      creatingConnection = false
      connectionStartNodeId = None
    }

  private def installKeyboardHandler(): Unit =
    listenTo(keys)

    reactions += { case KeyPressed(_, Key.Delete, _, _) =>
      selectedNodeId.foreach(nodeId => dispatch(Msg.RemoveNode(nodeId)))

      selectedEdgeKey
        .flatMap(findEdge)
        .foreach(edge => dispatch(Msg.RemoveEdge(edge)))

      selectedNodeId = None
      selectedEdgeKey = None

      repaint()
    }

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
    ChannelForm.fromChannel(Channel.default(ChannelType.LAN))

  private def findEdge(key: (String, String)): Option[EdgeForm] =
    topology.edges.find(edge => (edge.from, edge.to) == key)

  override def paintComponent(g: Graphics2D): Unit =
    super.paintComponent(g)

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    g.translate(offsetX, offsetY)
    g.scale(zoom, zoom)

    WorkspaceRenderer.draw(g, topology, nodeViews, nodeRadius, selectedNodeId, selectedEdgeKey)

  private def rebuildNodeViews(): Unit =
    val previous = nodeViews.toMap

    nodeViews.clear()

    topology.nodes
      .sortBy(_.id)
      .zipWithIndex
      .foreach { case (node, index) =>
        val row = index / 6
        val column = index % 6

        val view = previous.get(node.id) match
          case Some(old) => ViewNode(form = node, x = old.x, y = old.y)
          case None =>
            ViewNode(form = node, x = 80.0 + column * 130.0, y = 80.0 + row * 130.0)

        nodeViews.put(node.id, view)
      }
