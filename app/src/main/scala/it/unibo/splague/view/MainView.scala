package it.unibo.splague.view

import it.unibo.splague.update.Mvu.Screen.{Menu, Simulation}
import it.unibo.splague.update.Mvu.{ModelState, Msg}
import java.awt.Dimension
import scala.swing.{BoxPanel, Component, MainFrame, Orientation}

class MainView extends MainFrame with Renderer:
  title = "SPlagueNet"

  private val contentPanel = new BoxPanel(Orientation.Vertical)

  contents = contentPanel
  size = Dimension(800, 600)
  visible = true

  def update(component: Component): Unit =
    contentPanel.contents.clear()
    contentPanel.contents += component
    contentPanel.revalidate()
    contentPanel.repaint()

  def showView(modelState: ModelState, dispatch: Msg => Unit): Component =
    modelState.screen match
      case Menu               => MenuView.render(dispatch)
      case Simulation(engine) => SimulationView.render(engine, dispatch)
      case _                  => MenuView.render(dispatch)
