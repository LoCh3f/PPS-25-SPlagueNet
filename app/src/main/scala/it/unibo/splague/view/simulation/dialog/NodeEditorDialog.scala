package it.unibo.splague.view.simulation.dialog

import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.model.node.{NodeState, NodeType}
import it.unibo.splague.update.Msg
import it.unibo.splague.view.form.NodeForm

import scala.swing.{
  BorderPanel,
  BoxPanel,
  Button,
  CheckBox,
  ComboBox,
  Dialog,
  FlowPanel,
  GridPanel,
  Label,
  Orientation,
  TextField,
  Window
}
import scala.swing.event.ButtonClicked
import javax.swing.BorderFactory

/** Dialog responsible only for editing a [[NodeForm]]: field display, initialization from the
  * received form, validation, and dispatch of the already existing `Msg.AddNode` / `Msg.UpdateNode`
  * messages. It never touches the model directly.
  */
object NodeEditorDialog:

  /** `NodeType` is a sealed trait (not a Scala 3 enum), so it has no `.values`. This is the
    * exhaustive list of the case objects actually defined in
    * `it.unibo.splague.model.node.NodeType`.
    */
  private val nodeTypes: Vector[NodeType] =
    Vector(
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
    val dialog = DialogUtils.createModalDialog(
      owner,
      if isNew then "Create node" else s"Edit node ${initial.id}"
    )

    val idField = new TextField(initial.id, 16):
      enabled = isNew

    val nodeTypeCombo = new ComboBox[NodeType](nodeTypes):
      selection.item = initial.nodeType

    val stateCombo = new ComboBox[NodeState](NodeState.values.toSeq):
      selection.item = initial.state

    val patchLevelField = new TextField(initial.patchLevel, 10)
    val defenseLevelField = new TextField(initial.defenseLevel, 10)
    val workloadField = new TextField(initial.workload, 10)

    val vectorCheckboxes: Vector[(PropagationVector, CheckBox)] =
      PropagationVector.values.toVector.map { vector =>
        vector -> new CheckBox(vector.toString):
          selected = initial.vectors.contains(vector)
      }

    val fields = DialogUtils.createFieldGrid()
    DialogUtils.addField(fields, "ID", idField)
    DialogUtils.addField(fields, "Type", nodeTypeCombo)
    DialogUtils.addField(fields, "State", stateCombo)
    DialogUtils.addField(fields, "Patch level", patchLevelField)
    DialogUtils.addField(fields, "Defense level", defenseLevelField)
    DialogUtils.addField(fields, "Workload", workloadField)
    val vectorsPanel = new GridPanel(0, 1):
      vectorCheckboxes.foreach { case (_, checkbox) => contents += checkbox }

    DialogUtils.addField(fields, "Vectors", vectorsPanel)

    val save = new Button("Save")
    val cancel = new Button("Cancel")

    def onSave(): Unit =
      val selectedVectors: Set[PropagationVector] =
        vectorCheckboxes.collect { case (vector, checkbox) if checkbox.selected => vector }.toSet

      val updated = initial.copy(
        id = idField.text.trim,
        nodeType = nodeTypeCombo.selection.item,
        patchLevel = patchLevelField.text,
        defenseLevel = defenseLevelField.text,
        state = stateCombo.selection.item,
        workload = workloadField.text,
        vectors = selectedVectors
      )

      if updated.id.isEmpty then
        Dialog.showMessage(
          fields,
          "Node ID cannot be empty",
          title = "Invalid input",
          messageType = Dialog.Message.Error
        )
      else
        if isNew then dispatch(Msg.AddNode(updated))
        else dispatch(Msg.UpdateNode(updated))
        dialog.dispose()

    DialogUtils.setupButtonListeners(save, cancel, onSave, () => dialog.dispose())

    DialogUtils.displayDialog(dialog, fields, save, cancel, screenX, screenY)
