package it.unibo.splague.update

import it.unibo.splague.model.malware.{MalwareKind, PayloadSeverityLevel, PropagationVector}
import it.unibo.splague.persistence.FileFormat
import it.unibo.splague.view.form.{
  AwarenessForm,
  CountermeasureForm,
  EdgeForm,
  MalwareForm,
  NodeForm,
  ScenarioForm
}

enum Msg:

  case GoToMenu
  case GoToSimulation
  case SelectScenario(name: String)

  case UpdateScenarioName(scenario: ScenarioForm)

  case AddNode(node: NodeForm)
  case RemoveNode(nodeId: String)
  case UpdateNode(node: NodeForm)

  case AddEdge(edge: EdgeForm)
  case RemoveEdge(edge: EdgeForm)
  case UpdateEdge(edge: EdgeForm)

  case UpdateMalware(malware: MalwareForm)

  case UpdateAwareness(awareness: AwarenessForm)
  case UpdateCountermeasure(countermeasure: CountermeasureForm)

  case SaveScenario
  case CancelScenario
  case StartSimulation
  case SimulationStep

  // export/import scenario
  case ExportScenario(format: FileFormat)
