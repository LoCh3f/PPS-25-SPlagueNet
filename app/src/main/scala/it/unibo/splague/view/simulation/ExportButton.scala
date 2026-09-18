package it.unibo.splague.view.simulation

import it.unibo.splague.persistence.FileFormat
import it.unibo.splague.update.Msg

import javax.swing.{JButton, JMenuItem, JPopupMenu}

object ExportButton:

  def apply(dispatch: Msg => Unit): JButton =
    val exportMenu = new JPopupMenu()

    val jsonItem = new JMenuItem("json")
    jsonItem.addActionListener(_ => dispatch(Msg.ExportScenario(FileFormat.Json)))

    val txtItem = new JMenuItem("txt")
    jsonItem.addActionListener(_ => dispatch(Msg.ExportScenario(FileFormat.Txt)))

    val ymlItem = new JMenuItem("html")
    jsonItem.addActionListener(_ => dispatch(Msg.ExportScenario(FileFormat.Html)))

    exportMenu.add(jsonItem)
    exportMenu.add(txtItem)
    exportMenu.add(ymlItem)

    val exportButton = new JButton("Export Scenario ▾")
    exportButton.addActionListener(_ => exportMenu.show(exportButton, 0, exportButton.getHeight()))
    exportButton
