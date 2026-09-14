package it.unibo.splague.view.simulation

import it.unibo.splague.model.connection.Connection.ChannelType
import it.unibo.splague.update.Msg
import it.unibo.splague.view.form.ChannelForm
import it.unibo.splague.view.form.EdgeForm

import java.awt.BorderLayout
import java.awt.Dialog
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.Window
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

/** Dialog responsible only for editing an [[EdgeForm]]: field display, initialization from the
  * received form, validation, and dispatch of the already existing `Msg.AddEdge` / `Msg.UpdateEdge`
  * messages. Edge identity is the (from, to) pair carried by the form itself; there is no separate
  * edge id in `EdgeForm`, so source and destination are shown disabled, as in the previous
  * behavior.
  *
  * `EdgeForm.protocol` has no editable concrete `ApplicationProtocol` instances available in the
  * project, so it is preserved unchanged through `copy` rather than edited here.
  */
object EdgeEditorDialog:

  def show(
      owner: Window,
      initial: EdgeForm,
      screenX: Int,
      screenY: Int,
      isNew: Boolean,
      dispatch: Msg => Unit
  ): Unit =
    val dialog =
      new JDialog(
        owner,
        if isNew then "Create edge"
        else s"Edit edge ${initial.from} -> ${initial.to}",
        Dialog.ModalityType.APPLICATION_MODAL
      )

    val fromField =
      new JTextField(
        initial.from,
        14
      )

    fromField.setEnabled(false)

    val toField =
      new JTextField(
        initial.to,
        14
      )

    toField.setEnabled(false)

    val channelTypeCombo =
      new JComboBox[ChannelType](ChannelType.values)

    channelTypeCombo.setSelectedItem(initial.channel.channelType)

    val bandwidthField =
      new JTextField(
        initial.channel.bandwidth,
        10
      )

    val latencyField =
      new JTextField(
        initial.channel.latency,
        10
      )

    val jitterField =
      new JTextField(
        initial.channel.jitter,
        10
      )

    val packetLossField =
      new JTextField(
        initial.channel.packetLoss,
        10
      )

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

    fields.add(new JLabel("From"))
    fields.add(fromField)

    fields.add(new JLabel("To"))
    fields.add(toField)

    fields.add(new JLabel("Channel type"))
    fields.add(channelTypeCombo)

    fields.add(new JLabel("Bandwidth"))
    fields.add(bandwidthField)

    fields.add(new JLabel("Latency"))
    fields.add(latencyField)

    fields.add(new JLabel("Jitter"))
    fields.add(jitterField)

    fields.add(new JLabel("Packet loss"))
    fields.add(packetLossField)

    val save =
      new JButton("Save")

    save.addActionListener(_ =>
      val updatedChannel =
        ChannelForm(
          channelType = channelTypeCombo.getSelectedItem.asInstanceOf[ChannelType],
          bandwidth = bandwidthField.getText,
          latency = latencyField.getText,
          jitter = jitterField.getText,
          packetLoss = packetLossField.getText
        )

      val updated =
        initial.copy(
          channel = updatedChannel
        )

      if isNew then
        dispatch(
          Msg.AddEdge(updated)
        )
      else
        dispatch(
          Msg.UpdateEdge(updated)
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
