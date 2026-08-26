package it.unibo.splague.update

import it.unibo.splague.update.Mvu.{ModelState, Msg, update}
import it.unibo.splague.view.MainView

class Runtime(initialModel: ModelState, view: MainView):
  private var model: ModelState = initialModel
  private val timer = javax.swing.Timer(200, _ => dispatch(Msg.Step))

  def dispatch(msg: Msg): Unit =
    model = update(msg, model)
    // view.update()

  def pause(): Unit = timer.stop()
  def start(): Unit = timer.start()
