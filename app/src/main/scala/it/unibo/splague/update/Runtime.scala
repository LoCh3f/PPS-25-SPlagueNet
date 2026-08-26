package it.unibo.splague.update

import it.unibo.splague.update.Mvu.{ModelState, Msg, update}
import it.unibo.splague.view.{MainView, Renderer}

class Runtime(initialModel: ModelState, view: Renderer):
  private var model: ModelState = initialModel
  private val timer = javax.swing.Timer(200, _ => dispatch(Msg.Step))

  render()

  def dispatch(msg: Msg): Unit =
    model = update(msg, model)
    render()

  private def render(): Unit =
    val component = view.showView(model, dispatch)
    view.update(component)

  def pause(): Unit = timer.stop()
  def start(): Unit = timer.start()
