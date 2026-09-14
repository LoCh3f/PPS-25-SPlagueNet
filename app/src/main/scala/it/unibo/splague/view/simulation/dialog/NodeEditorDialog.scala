package it.unibo.splague.view.simulation.dialog

import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.model.node.{NodeState, NodeType}
import it.unibo.splague.update.Msg
import it.unibo.splague.view.form.NodeForm

import java.awt.BorderLayout
import java.awt.Dialog
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.Window
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextField

/** Dialog responsible only for editing a [[NodeForm]]: field display, initialization from the
  * received form, validation, and dispatch of the already existing `Msg.AddNode` / `Msg.UpdateNode`
  * messages. It never touches the model directly.
  */
object NodeEditorDialog:

  /** `NodeType` is a sealed trait (not a Scala 3 enum), so it has no `.values`. This is the
    * exhaustive list of the case objects actually defined in
    * `it.unibo.splague.model.node.NodeType`.
    */
  private val nodeTypes: Array[NodeType] =
    Array(
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
    val dialog =
      new JDialog(
        owner,
        if isNew then "Create node"
        else s"Edit node ${initial.id}",
        Dialog.ModalityType.APPLICATION_MODAL
      )

    val idField =
      new JTextField(
        initial.id,
        16
      )

    if !isNew then idField.setEnabled(false)

    val nodeTypeCombo =
      new JComboBox[NodeType](nodeTypes)

    nodeTypeCombo.setSelectedItem(initial.nodeType)

    val stateCombo =
      new JComboBox[NodeState](NodeState.values)

    stateCombo.setSelectedItem(initial.state)

    val patchLevelField =
      new JTextField(
        initial.patchLevel,
        10
      )

    val defenseLevelField =
      new JTextField(
        initial.defenseLevel,
        10
      )

    val workloadField =
      new JTextField(
        initial.workload,
        10
      )

    val vectorCheckboxes: Vector[(PropagationVector, JCheckBox)] =
      PropagationVector.values.toVector.map { vector =>
        val checkbox =
          new JCheckBox(
            vector.toString,
            initial.vectors.contains(vector)
          )

        vector -> checkbox
      }

    val fields =
      new JPanel(
        new GridLayout(
          0,
          2,
          6,
          6
        )
      )

    fields.setBorder(
      BorderFactory.createEmptyBorder(
        8,
        8,
        8,
        8
      )
    )

    fields.add(new JLabel("ID"))
    fields.add(idField)

    fields.add(new JLabel("Type"))
    fields.add(nodeTypeCombo)

    fields.add(new JLabel("State"))
    fields.add(stateCombo)

    fields.add(new JLabel("Patch level"))
    fields.add(patchLevelField)

    fields.add(new JLabel("Defense level"))
    fields.add(defenseLevelField)

    fields.add(new JLabel("Workload"))
    fields.add(workloadField)

    fields.add(new JLabel("Vectors"))

    val vectorsPanel =
      new JPanel(
        new GridLayout(
          0,
          1
        )
      )

    vectorCheckboxes.foreach { case (_, checkbox) =>
      vectorsPanel.add(checkbox)
    }

    fields.add(vectorsPanel)

    val save =
      new JButton("Save")

    save.addActionListener(_ =>
      val selectedVectors: Set[PropagationVector] =
        vectorCheckboxes.collect {
          case (vector, checkbox) if checkbox.isSelected => vector
        }.toSet

      val updated =
        initial.copy(
          id = idField.getText.trim,
          nodeType = nodeTypeCombo.getSelectedItem.asInstanceOf[NodeType],
          patchLevel = patchLevelField.getText,
          defenseLevel = defenseLevelField.getText,
          state = stateCombo.getSelectedItem.asInstanceOf[NodeState],
          workload = workloadField.getText,
          vectors = selectedVectors
        )

      if updated.id.isEmpty then
        JOptionPane.showMessageDialog(
          dialog,
          "Node ID cannot be empty",
          "Invalid input",
          JOptionPane.ERROR_MESSAGE
        )
      else
        if isNew then
          dispatch(
            Msg.AddNode(updated)
          )
        else
          dispatch(
            Msg.UpdateNode(updated)
          )

        dialog.dispose()
    )

    val cancel =
      new JButton("Cancel")

    cancel.addActionListener(_ => dialog.dispose())

    val buttons =
      new JPanel(
        new FlowLayout(
          FlowLayout.RIGHT
        )
      )

    buttons.add(cancel)
    buttons.add(save)

    val container =
      new JPanel(
        new BorderLayout(
          8,
          8
        )
      )

    container.setBorder(
      BorderFactory.createEmptyBorder(
        8,
        8,
        8,
        8
      )
    )

    container.add(
      fields,
      BorderLayout.CENTER
    )

    container.add(
      buttons,
      BorderLayout.SOUTH
    )

    dialog.setContentPane(container)
    dialog.pack()
    dialog.setLocation(screenX, screenY)
    dialog.setResizable(true)
    dialog.setVisible(true)
