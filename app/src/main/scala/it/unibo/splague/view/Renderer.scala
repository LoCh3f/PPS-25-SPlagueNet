package it.unibo.splague.view

import it.unibo.splague.AppState
import it.unibo.splague.update.Msg

import scala.swing.Component

trait Renderer:
  def update(component: Component): Unit
  def showView(state: AppState, dispatch: Msg => Unit): Component
