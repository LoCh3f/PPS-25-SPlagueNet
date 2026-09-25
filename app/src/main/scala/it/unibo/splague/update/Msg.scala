package it.unibo.splague.update

import it.unibo.splague.model.malware.{MalwareKind, PayloadSeverityLevel, PropagationVector}
import it.unibo.splague.persistence.FileFormat
import it.unibo.splague.view.form.countermeasure.CountermeasureForm
import it.unibo.splague.view.form.{AwarenessForm, EdgeForm, MalwareForm, NodeForm, ScenarioForm}

import java.nio.file.Path

/** One of the shape generators in `it.unibo.splague.dsl.TopologyShapes`, referenced by `Msg`
  * without pulling the `dsl` package into the message set.
  */
enum TopologyShape:
  case Star, Ring, Mesh

enum Msg:

  case GoToMenu
  case GoToSimulation
  case GoToReport
  case SelectScenario(name: String)

  case UpdateScenarioName(scenario: ScenarioForm)

  case AddNode(node: NodeForm)
  case RemoveNode(nodeId: String)
  case UpdateNode(node: NodeForm)

  case AddEdge(edge: EdgeForm)
  case RemoveEdge(edge: EdgeForm)
  case UpdateEdge(edge: EdgeForm)

  case AddShape(shape: TopologyShape)

  case UpdateMalware(malware: MalwareForm)

  case UpdateAwareness(awareness: AwarenessForm)
  case UpdateCountermeasure(countermeasure: CountermeasureForm)

  case SaveScenario
  case CancelScenario
  case StartSimulation
  case SimulationStep
  case ResetSimulation

  // export/import scenario
  case ExportScenario(format: FileFormat)
  case ImportScenario(format: FileFormat, path: Path)
