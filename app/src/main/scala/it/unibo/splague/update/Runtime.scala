package it.unibo.splague.update

import it.unibo.splague.AppState
import it.unibo.splague.update.Msg
import it.unibo.splague.update.Mvu.update
import it.unibo.splague.view.Renderer

import javax.swing.Timer

// $COVERAGE-OFF$
final class Runtime(
    initialState: AppState,
    view: Renderer
):

  private var state: AppState = initialState

  private val tickIntervalMillis = 2500

  private val timer =
    new Timer(
      tickIntervalMillis,
      _ => dispatch(Msg.SimulationStep)
    )

  render()

  def dispatch(msg: Msg): Unit =
    val nextState = update(msg, state)
    if nextState != state then
      state = nextState
      render()

  private def render(): Unit =
    val component =
      view.showView(state, dispatch)

    view.update(component)

  def pause(): Unit =
    timer.stop()

  def start(): Unit =
    timer.start()
// $COVERAGE-ON$
