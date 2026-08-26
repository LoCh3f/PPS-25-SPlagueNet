package it.unibo.splague.view

import java.awt.Dimension
import scala.swing.{BoxPanel, Component, MainFrame, Orientation}

class MainView extends MainFrame:
  title = "SPlagueNet"

  // Il pannello interno con il layout verticale
  private val contentPanel = new BoxPanel(Orientation.Vertical)

  contents = contentPanel
  size = Dimension(800, 600)
  visible = true // Fondamentale per renderla visibile!

  def update(component: Component): Unit =
    contentPanel.contents.clear()
    contentPanel.contents += component
    contentPanel.revalidate()
    contentPanel.repaint()
