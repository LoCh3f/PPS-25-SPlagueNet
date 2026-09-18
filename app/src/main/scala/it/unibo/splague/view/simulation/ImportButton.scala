package it.unibo.splague.view.simulation

import it.unibo.splague.persistence.FileFormat
import it.unibo.splague.update.Msg

import javax.swing.{JButton, JMenuItem, JPopupMenu}

object ImportButton:
  def apply(dispatch: Msg => Unit): JButton =
    val importMenu = new JPopupMenu()

    val jsonItem = new JMenuItem("json")
    jsonItem.addActionListener(_ => dispatch(Msg.ExportScenario(FileFormat.Json)))

    importMenu.add(jsonItem)

    val importButton = new JButton("Import Scenario ▾")
    importButton.addActionListener(_ => importMenu.show(importButton, 0, importButton.getHeight()))
    importButton
