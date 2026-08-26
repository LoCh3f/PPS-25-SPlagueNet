package it.unibo.splague.view

import it.unibo.splague.update.Mvu.Screen.{Menu, Simulation}
import it.unibo.splague.update.Mvu.{ModelState, Msg}
import java.awt.Dimension
import scala.swing.{Action, BoxPanel, Button, Component, Label, MainFrame, Orientation}

trait Renderer:
  def update(component: Component): Unit
  def showView(modelState: ModelState, dispatch: Msg => Unit): Component

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
      case Menu               => showMenu(dispatch)
      case Simulation(engine) => showSimuation(engine, dispatch)

  private def showMenu(dispatch: Msg => Unit): Component =
    new BoxPanel(Orientation.Vertical):

      contents += new Label("Welcome to SPlagueNet") {}

      contents += new Button(Action("Go to Simulation") {
        dispatch(Msg.GoToSimulation)
      })

      contents += new Button(Action("Exit") {
        sys.exit(0)
      })

  private def showSimuation(engine: Any, dispatch: Msg => Unit): Component =
    new BoxPanel(Orientation.Vertical):
      contents += new Label(s"Simulation Running: $engine")
      contents += new Button(Action("Back to menu") {
        dispatch(Msg.ReturnToMenu)
      })
