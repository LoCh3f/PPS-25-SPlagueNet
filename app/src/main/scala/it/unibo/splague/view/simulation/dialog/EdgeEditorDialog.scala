package it.unibo.splague.view.simulation.dialog

import it.unibo.splague.model.connection.Connection.ChannelType
import it.unibo.splague.update.Msg
import it.unibo.splague.view.form.ChannelForm
import it.unibo.splague.view.form.EdgeForm

import scala.swing.{
  BorderPanel,
  Button,
  ComboBox,
  Dialog,
  FlowPanel,
  GridPanel,
  Label,
  TextField,
  Window
}
import scala.swing.event.ButtonClicked
import javax.swing.BorderFactory

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
    val dialog = DialogUtils.createModalDialog(
      owner,
      if isNew then "Create edge" else s"Edit edge ${initial.from} -> ${initial.to}"
    )

    val fromField = new TextField(initial.from, 14):
      enabled = false

    val toField = new TextField(initial.to, 14):
      enabled = false

    val channelTypeCombo = new ComboBox[ChannelType](ChannelType.values.toSeq):
      selection.item = initial.channel.channelType

    val bandwidthField = new TextField(initial.channel.bandwidth, 10)
    val latencyField = new TextField(initial.channel.latency, 10)
    val jitterField = new TextField(initial.channel.jitter, 10)
    val packetLossField = new TextField(initial.channel.packetLoss, 10)

    val fields = DialogUtils.createFieldGrid()
    fields.contents += new Label("From")
    fields.contents += fromField

    fields.contents += new Label("To")
    fields.contents += toField

    fields.contents += new Label("Channel type")
    fields.contents += channelTypeCombo

    fields.contents += new Label("Bandwidth")
    fields.contents += bandwidthField

    fields.contents += new Label("Latency")
    fields.contents += latencyField

    fields.contents += new Label("Jitter")
    fields.contents += jitterField

    fields.contents += new Label("Packet loss")
    fields.contents += packetLossField

    val save = new Button("Save")
    val cancel = new Button("Cancel")

    val onSave = () => {
      val updatedChannel = ChannelForm(
        channelType = channelTypeCombo.selection.item,
        bandwidth = bandwidthField.text,
        latency = latencyField.text,
        jitter = jitterField.text,
        packetLoss = packetLossField.text
      )

      val updated = initial.copy(channel = updatedChannel)

      if isNew then dispatch(Msg.AddEdge(updated))
      else dispatch(Msg.UpdateEdge(updated))

      dialog.dispose()
    }

    DialogUtils.setupButtonListeners(save, cancel, onSave, () => dialog.dispose())

    DialogUtils.displayDialog(dialog, fields, save, cancel, screenX, screenY)
