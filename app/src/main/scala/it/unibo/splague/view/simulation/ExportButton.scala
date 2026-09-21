package it.unibo.splague.view.simulation

import it.unibo.splague.persistence.FileFormat
import it.unibo.splague.update.Msg

import scala.swing.{Button, Component, MenuItem, PopupMenu}
import scala.swing.event.ButtonClicked

object ExportButton:

  def apply(dispatch: Msg => Unit): Component =
    val exportButton = new Button("Export Scenario \u25be"):
      focusable = false

    val jsonItem = new MenuItem("json")
    val txtItem = new MenuItem("txt")

    val exportMenu = new PopupMenu:
      contents += jsonItem
      contents += txtItem

    jsonItem.listenTo(jsonItem)
    jsonItem.reactions += { case ButtonClicked(_) =>
      dispatch(Msg.ExportScenario(FileFormat.Json))
    }

    txtItem.listenTo(txtItem)
    txtItem.reactions += { case ButtonClicked(_) =>
      dispatch(Msg.ExportScenario(FileFormat.Txt))
    }

    exportButton.listenTo(exportButton)
    exportButton.reactions += { case ButtonClicked(_) =>
      exportMenu.show(exportButton, 0, exportButton.bounds.height)
    }

    exportButton
