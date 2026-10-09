package it.unibo.splague.view

import scala.swing.{Action, Button, GridPanel, Label}

/** Shared UI building blocks used across multiple views. Each helper is a pure factory: no state,
  * no side effects beyond creating a Swing component.
  */
object ViewHelpers:

  /** A button whose only job is to run `action` when clicked. Handles the `Action` wrapper and
    * full-width sizing so callers don't repeat the boilerplate.
    */
  def actionButton(text: String)(action: => Unit): Button =
    new Button(Action(text) { action }):
      maximumSize = new java.awt.Dimension(Short.MaxValue, Short.MaxValue)

  /** A two-column label/value row for summary panels (e.g. ReportView). `label` is left-aligned,
    * `value` is plain text on the right cell.
    */
  def labelValueRow(label: String, value: String): GridPanel =
    new GridPanel(1, 2):
      contents += new Label(label)
      contents += new Label(value)
