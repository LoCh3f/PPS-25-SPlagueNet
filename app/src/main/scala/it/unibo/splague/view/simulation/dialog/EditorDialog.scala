package it.unibo.splague.view.simulation.dialog

import scala.swing.{Button, GridPanel, Window}

/** Base template for node and edge editor dialogs. Subclasses provide the field grid, the save
  * logic, and the dialog title; this trait handles the modal window lifecycle and the save/cancel
  * wiring.
  *
  * Usage: implement [[buildFields]], [[onSave]], and [[dialogTitle]]. Then call [[show]] to run the
  * dialog.
  */
trait EditorDialog:

  /** The title shown in the dialog window title bar. */
  protected def dialogTitle: String

  /** Builds and returns the populated field grid. Called once per [[show]] invocation.
    */
  protected def buildFields(): GridPanel

  /** Called when the user clicks Save. Should read field values, dispatch the appropriate message,
    * and call `dispose()` on success. Receives `dispose` as a parameter so implementors don't need
    * a reference to the dialog itself.
    */
  protected def onSave(dispose: () => Unit): Unit

  /** Opens the modal dialog at the given screen position. Blocks until the user closes it (modal).
    */
  final def show(owner: Window, screenX: Int, screenY: Int): Unit =
    val dialog = DialogUtils.createModalDialog(owner, dialogTitle)
    val fields = buildFields()
    val save = new Button("Save")
    val cancel = new Button("Cancel")

    DialogUtils.setupButtonListeners(
      save,
      cancel,
      onSave = () => onSave(() => dialog.dispose()),
      onCancel = () => dialog.dispose()
    )

    DialogUtils.displayDialog(dialog, fields, save, cancel, screenX, screenY)
