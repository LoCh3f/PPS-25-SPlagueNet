package it.unibo.splague.view.simulation.dialog

import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.model.node.{NodeState, NodeType}
import it.unibo.splague.update.Msg
import it.unibo.splague.view.form.NodeForm

import scala.swing.*

object NodeEditorDialog:

  private val nodeTypes: Vector[NodeType] = Vector(
    NodeType.Workstation,
    NodeType.Server,
    NodeType.Router,
    NodeType.IoTDevice,
    NodeType.MobileDevice
  )

  def show(
      owner: Window,
      initial: NodeForm,
      screenX: Int,
      screenY: Int,
      isNew: Boolean,
      dispatch: Msg => Unit
  ): Unit =
    val dialog = new EditorDialog:

      override protected val dialogTitle: String =
        if isNew then "Create node" else s"Edit node ${initial.id}"

      private val idField = new TextField(initial.id, 16)
      idField.enabled = isNew

      private val nodeTypeCombo = new ComboBox[NodeType](nodeTypes)
      nodeTypeCombo.selection.item = initial.nodeType

      private val stateCombo = new ComboBox[NodeState](NodeState.values.toSeq)
      stateCombo.selection.item = initial.state

      private val patchField = new TextField(initial.patchLevel, 10)
      private val defenseField = new TextField(initial.defenseLevel, 10)
      private val workloadField = new TextField(initial.workload, 10)

      private val vectorChecks: Vector[(PropagationVector, CheckBox)] =
        PropagationVector.values.toVector.map { v =>
          val cb = new CheckBox(v.toString)
          cb.selected = initial.vectors.contains(v)
          v -> cb
        }

      override protected def buildFields(): GridPanel =
        val grid = DialogUtils.createFieldGrid()
        DialogUtils.addField(grid, "ID", idField)
        DialogUtils.addField(grid, "Type", nodeTypeCombo)
        DialogUtils.addField(grid, "State", stateCombo)
        DialogUtils.addField(grid, "Patch level", patchField)
        DialogUtils.addField(grid, "Defense level", defenseField)
        DialogUtils.addField(grid, "Workload", workloadField)
        val vectorsPanel = new GridPanel(0, 1):
          vectorChecks.foreach { case (_, cb) => contents += cb }
        DialogUtils.addField(grid, "Vectors", vectorsPanel)
        grid

      override protected def onSave(dispose: () => Unit): Unit =
        val selectedVectors: Set[PropagationVector] =
          vectorChecks.collect { case (v, cb) if cb.selected => v }.toSet

        val updated = initial.copy(
          id = idField.text.trim,
          nodeType = nodeTypeCombo.selection.item,
          patchLevel = patchField.text,
          defenseLevel = defenseField.text,
          state = stateCombo.selection.item,
          workload = workloadField.text,
          vectors = selectedVectors
        )

        if updated.id.isEmpty then
          Dialog.showMessage(
            idField,
            "Node ID cannot be empty",
            title = "Invalid input",
            messageType = Dialog.Message.Error
          )
        else
          if isNew then dispatch(Msg.AddNode(updated))
          else dispatch(Msg.UpdateNode(updated))
          dispose()

    dialog.show(owner, screenX, screenY)
