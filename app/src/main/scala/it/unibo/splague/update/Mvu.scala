package it.unibo.splague.update

object Mvu:
  enum Msg:
    case Step
    case ReturnToMenu

  enum Screen:
    case Menu
    case Simulation(engine: Any)

  case class ModelState(screen: Screen)

  def update(msg: Msg, modelState: ModelState): ModelState =
    (msg, modelState.screen) match
      case (Msg.Step, Screen.Simulation(engine)) => modelState.copy( /*???*/ )
      case _                                     => modelState

  object ModelState:
    def init(): ModelState = ModelState(screen = Screen.Menu)
