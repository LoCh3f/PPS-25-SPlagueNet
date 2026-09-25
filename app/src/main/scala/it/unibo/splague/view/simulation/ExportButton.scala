package it.unibo.splague.view.simulation

import it.unibo.splague.persistence.FileFormat
import it.unibo.splague.update.Msg

import scala.swing.Component

object ExportButton:

  def apply(dispatch: Msg => Unit): Component =
    DropdownButton(
      "Export Scenario ▾",
      Seq(
        "json" -> (() => dispatch(Msg.ExportScenario(FileFormat.Json))),
        "txt" -> (() => dispatch(Msg.ExportScenario(FileFormat.Txt)))
      )
    )
