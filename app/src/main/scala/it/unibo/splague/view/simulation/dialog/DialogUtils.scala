package it.unibo.splague.view.simulation.dialog

import scala.swing.{BorderPanel, Button, Dialog, FlowPanel, GridPanel, Panel, Window}
import scala.swing.event.ButtonClicked
import javax.swing.BorderFactory

/** Shared utilities for editor dialogs to eliminate code duplication. */
object DialogUtils:

  /** Creates a standard 2-column field grid panel with consistent spacing and borders. */
  def createFieldGrid(): GridPanel =
    new GridPanel(0, 2):
      hGap = 6
      vGap = 6
      border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

  /** Sets up button listeners for save and cancel buttons. */
  def setupButtonListeners(
      save: Button,
      cancel: Button,
      onSave: () => Unit,
      onCancel: () => Unit
  ): Unit =
    save.listenTo(save)
    save.reactions += { case ButtonClicked(_) => onSave() }

    cancel.listenTo(cancel)
    cancel.reactions += { case ButtonClicked(_) => onCancel() }

  /** Displays a dialog with the given fields, save, and cancel buttons. Positions it on-screen and
    * makes it visible.
    */
  def displayDialog(
      dialog: Dialog,
      fields: Panel,
      save: Button,
      cancel: Button,
      screenX: Int,
      screenY: Int
  ): Unit =
    val buttons = new FlowPanel(FlowPanel.Alignment.Right)(cancel, save)
    val container = new BorderPanel:
      border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
      layout(fields) = BorderPanel.Position.Center
      layout(buttons) = BorderPanel.Position.South

    dialog.contents = container
    dialog.pack()
    dialog.peer.setLocation(screenX, screenY)
    dialog.resizable = true
    dialog.visible = true

  /** Creates a standard modal dialog for editing. */
  def createModalDialog(owner: Window, dialogTitle: String): Dialog =
    new Dialog(owner):
      title = dialogTitle
      modal = true
