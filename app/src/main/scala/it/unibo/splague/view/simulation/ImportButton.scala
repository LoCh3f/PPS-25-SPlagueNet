package it.unibo.splague.view.simulation

import it.unibo.splague.persistence.{ExportPaths, FileFormat}
import it.unibo.splague.update.Msg

import scala.swing.{Component, FileChooser}

object ImportButton:

  def apply(dispatch: Msg => Unit): Component =
    DropdownButton(
      "Import Scenario ▾",
      Seq("json" -> (() => importJson(dispatch)))
    )

  private def importJson(dispatch: Msg => Unit): Unit =
    val chooser = new FileChooser(ExportPaths.baseDirectory.toFile)
    chooser.fileFilter = new javax.swing.filechooser.FileNameExtensionFilter("JSON Files", "json")

    chooser.showOpenDialog(null) match
      case FileChooser.Result.Approve =>
        dispatch(Msg.ImportScenario(FileFormat.Json, chooser.selectedFile.toPath))
      case _ => ()
