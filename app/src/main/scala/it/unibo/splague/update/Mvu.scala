package it.unibo.splague.update

import it.unibo.splague.AppState

/** The single entry point for every `AppState` transition (MVU's `update`). Dispatches each `Msg`
  * to the handler object responsible for its category, so each handler stays focused on one concern
  * (navigation, scenario-form editing, scenario lifecycle, simulation lifecycle, scenario
  * import/export) instead of this file growing without bound as `Msg` cases are added.
  */
// $COVERAGE-OFF$
object Mvu:

  def update(msg: Msg, state: AppState): AppState = msg match

    case Msg.GoToMenu | Msg.GoToSimulation | Msg.GoToReport | Msg.ImportReport(_) =>
      NavigationUpdate.update(msg, state)

    case Msg.UpdateScenarioName(_) | Msg.AddNode(_) | Msg.UpdateNode(_) | Msg.RemoveNode(_) |
        Msg.AddEdge(_) | Msg.UpdateEdge(_) | Msg.RemoveEdge(_) | Msg.AddShape(_) |
        Msg.UpdateMalware(_) | Msg.UpdateAwareness(_) | Msg.UpdateCountermeasure(_) =>
      ScenarioFormUpdate.update(msg, state)

    case Msg.SelectScenario(_) | Msg.SaveScenario | Msg.CancelScenario =>
      ScenarioLifecycleUpdate.update(msg, state)

    case Msg.StartSimulation | Msg.SimulationStep | Msg.ToggleSimulationPause |
        Msg.ResetSimulation =>
      SimulationUpdate.update(msg, state)

    case Msg.ExportScenario(_) | Msg.ImportScenario(_, _) =>
      ScenarioIOUpdate.update(msg, state)
// $COVERAGE-ON$
