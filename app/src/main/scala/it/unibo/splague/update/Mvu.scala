package it.unibo.splague.update

object Mvu:
  enum Msg:
    case Step

  enum Screen:
    case Menu
    case Simulation(engine: Any)

  case class ModelState(screen: Screen)

  object ModelState:
    def init(): ModelState = ModelState(screen = Screen.Menu)
