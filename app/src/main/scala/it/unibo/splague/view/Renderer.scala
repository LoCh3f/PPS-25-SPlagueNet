package it.unibo.splague.view

import it.unibo.splague.update.Mvu.{ModelState, Msg}
import scala.swing.Component

trait Renderer:
  def update(component: Component): Unit
  def showView(modelState: ModelState, dispatch: Msg => Unit): Component
