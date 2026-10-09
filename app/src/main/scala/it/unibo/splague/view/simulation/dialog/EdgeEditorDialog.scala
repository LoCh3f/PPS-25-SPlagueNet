package it.unibo.splague.view.simulation.dialog

import it.unibo.splague.model.connection.Connection.ChannelType
import it.unibo.splague.update.Msg
import it.unibo.splague.view.form.{ChannelForm, EdgeForm}

import scala.swing.*

object EdgeEditorDialog:

  def show(
      owner: Window,
      initial: EdgeForm,
      screenX: Int,
      screenY: Int,
      isNew: Boolean,
      dispatch: Msg => Unit
  ): Unit =
    val dialog = new EditorDialog:

      override protected val dialogTitle: String =
        if isNew then "Create edge" else s"Edit edge ${initial.from} -> ${initial.to}"

      private val fromField = new TextField(initial.from, 14)
      fromField.enabled = false

      private val toField = new TextField(initial.to, 14)
      toField.enabled = false

      private val channelCombo = new ComboBox[ChannelType](ChannelType.values.toSeq)
      channelCombo.selection.item = initial.channel.channelType

      private val bandwidthField = new TextField(initial.channel.bandwidth, 10)
      private val latencyField = new TextField(initial.channel.latency, 10)
      private val jitterField = new TextField(initial.channel.jitter, 10)
      private val packetLossField = new TextField(initial.channel.packetLoss, 10)

      override protected def buildFields(): GridPanel =
        val grid = DialogUtils.createFieldGrid()
        DialogUtils.addField(grid, "From", fromField)
        DialogUtils.addField(grid, "To", toField)
        DialogUtils.addField(grid, "Channel type", channelCombo)
        DialogUtils.addField(grid, "Bandwidth", bandwidthField)
        DialogUtils.addField(grid, "Latency", latencyField)
        DialogUtils.addField(grid, "Jitter", jitterField)
        DialogUtils.addField(grid, "Packet loss", packetLossField)
        grid

      override protected def onSave(dispose: () => Unit): Unit =
        val updatedChannel = ChannelForm(
          channelType = channelCombo.selection.item,
          bandwidth = bandwidthField.text,
          latency = latencyField.text,
          jitter = jitterField.text,
          packetLoss = packetLossField.text
        )
        val updated = initial.copy(channel = updatedChannel)
        if isNew then dispatch(Msg.AddEdge(updated))
        else dispatch(Msg.UpdateEdge(updated))
        dispose()

    dialog.show(owner, screenX, screenY)
