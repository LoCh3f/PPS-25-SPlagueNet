package it.unibo.splague.view

import it.unibo.splague.AppState
import it.unibo.splague.view.Screen
import it.unibo.splague.update.Msg
import it.unibo.splague.view.menu.MenuView
import it.unibo.splague.view.report.ReportView
import it.unibo.splague.view.simulation.SimulationView

import scala.swing.{BoxPanel, Component, MainFrame, Orientation}
import java.awt.Dimension

final class MainView extends MainFrame with Renderer:

  title = "SPlagueNet"

  private val contentPanel =
    new BoxPanel(Orientation.Vertical)

  contents = contentPanel
  size = new Dimension(800, 600)
  minimumSize = new Dimension(800, 600)
  visible = true

  override def showView(state: AppState, dispatch: Msg => Unit): Component =
    viewFor(state, dispatch)

  override def update(component: Component): Unit =
    contentPanel.contents.clear()
    contentPanel.contents += component
    contentPanel.revalidate()
    contentPanel.repaint()

  private def viewFor(
      state: AppState,
      dispatch: Msg => Unit
  ): Component =
    state.screen match
      case Screen.Menu =>
        MenuView.render(dispatch)

      case Screen.Simulation =>
        SimulationView.render(
          state = state,
          owner = this,
          dispatch = dispatch
        )

      case Screen.Report =>
        ReportView.render(
          state = state,
          dispatch = dispatch
        )
