package it.unibo.splague.view.simulation

import it.unibo.splague.persistence.{ExportPaths, FileFormat}
import it.unibo.splague.update.Msg

import java.io.File
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.{JButton, JFileChooser, JMenuItem, JPopupMenu}

object ImportButton:
  def apply(dispatch: Msg => Unit): JButton =
    val importMenu = new JPopupMenu()

    val jsonItem = new JMenuItem("json")
    jsonItem.addActionListener(_ =>
      val fileChooser = new JFileChooser(ExportPaths.baseDirectory.toFile)

      fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"))

      val result = fileChooser.showOpenDialog(null)

      if result == JFileChooser.APPROVE_OPTION then
        val selectedFile: File = fileChooser.getSelectedFile
        dispatch(Msg.ImportScenario(FileFormat.Json, selectedFile.toPath))
    )

    importMenu.add(jsonItem)

    val importButton = new JButton("Import Scenario ▾")
    importButton.addActionListener(_ => importMenu.show(importButton, 0, importButton.getHeight()))
    importButton
