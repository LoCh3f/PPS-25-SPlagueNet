package it.unibo.splague.view.simulation

import it.unibo.splague.persistence.{ExportPaths, FileFormat}
import it.unibo.splague.update.Msg

import scala.swing.{Button, Component, FileChooser, MenuItem, PopupMenu}
import scala.swing.event.ButtonClicked

object ImportButton:

  def apply(dispatch: Msg => Unit): Component =
    val importButton = new Button("Import Scenario \u25be"):
      focusable = false

    val jsonItem = new MenuItem("json")

    val importMenu = new PopupMenu:
      contents += jsonItem

    jsonItem.listenTo(jsonItem)
    jsonItem.reactions += { case ButtonClicked(_) =>
      val chooser = new FileChooser(ExportPaths.baseDirectory.toFile)
      chooser.fileFilter = new javax.swing.filechooser.FileNameExtensionFilter("JSON Files", "json")

      chooser.showOpenDialog(importButton) match
        case FileChooser.Result.Approve =>
          dispatch(Msg.ImportScenario(FileFormat.Json, chooser.selectedFile.toPath))
        case _ => ()
    }

    importButton.listenTo(importButton)
    importButton.reactions += { case ButtonClicked(_) =>
      importMenu.show(importButton, 0, importButton.bounds.height)
    }

    importButton
