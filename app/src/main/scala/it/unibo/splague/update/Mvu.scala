package it.unibo.splague.update

object Mvu:
  enum Msg:
    case Step
    case ReturnToMenu
    case GoToSimulation
    case GoToCreateView
    case CreateScenario
    case LoadTopology
    case SaveScenario

  enum Screen:
    case Menu
    case Simulation(engine: Any)
    case CreateScenario
    case SelectScenario

  case class ModelState(screen: Screen)

  def update(msg: Msg, modelState: ModelState): ModelState =
    (msg, modelState.screen) match
      case (Msg.Step, Screen.Simulation(engine)) => modelState.copy( /*???*/ )
      case (Msg.GoToSimulation, Screen.Menu) =>
        val initialEngine = "InitialEngine"
        modelState.copy(screen = Screen.Simulation(initialEngine))
      case (Msg.ReturnToMenu, _) =>
        modelState.copy(screen = Screen.Menu)
      case _ => modelState

  object ModelState:
    def init(): ModelState = ModelState(screen = Screen.Menu)
