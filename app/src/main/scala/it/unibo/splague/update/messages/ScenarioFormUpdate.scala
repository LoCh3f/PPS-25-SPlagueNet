package it.unibo.splague.update.messages

import it.unibo.splague.AppState
import it.unibo.splague.dsl.*
import it.unibo.splague.model.connection.Connection.ChannelType
import it.unibo.splague.model.node.{Node, NodeId, Topology}
import it.unibo.splague.update.{Msg, TopologyShape}
import it.unibo.splague.view.ValidationError
import it.unibo.splague.view.form.{AwarenessForm, EdgeForm, NodeForm, ScenarioForm}

/** Handles edits to the in-progress [[ScenarioForm]]: topology (nodes, edges, shapes), malware,
  * awareness, and countermeasure configuration. Covers [[Msg.UpdateScenarioName]], [[Msg.AddNode]],
  * [[Msg.UpdateNode]], [[Msg.RemoveNode]], [[Msg.AddEdge]], [[Msg.UpdateEdge]], [[Msg.RemoveEdge]],
  * [[Msg.AddShape]], [[Msg.UpdateMalware]], [[Msg.UpdateAwareness]], [[Msg.UpdateCountermeasure]].
  */
// $COVERAGE-OFF$
object ScenarioFormUpdate:

  def update(msg: Msg, state: AppState): AppState = msg match

    case Msg.UpdateScenarioName(form) =>
      updateForm(state) { s =>
        s.copy(
          name = form.name,
          seed = form.seed,
          maxIterations = form.maxIterations,
          startingNodeId = form.startingNodeId
        )
      }

    case Msg.AddNode(node) =>
      updateForm(state)(s => s.copy(topology = s.topology.copy(nodes = s.topology.nodes :+ node)))

    case Msg.UpdateNode(node) =>
      updateForm(state) { form =>
        form.copy(
          topology = form.topology.copy(
            nodes = form.topology.nodes.map { n =>
              if n.id == node.id then node else n
            }
          )
        )
      }

    case Msg.RemoveNode(nodeId) =>
      updateForm(state) { form =>
        val normalized = NodeId.normalize(nodeId)

        form.copy(
          topology = form.topology.copy(
            nodes = form.topology.nodes.filterNot { node =>
              NodeId.normalize(node.id) == normalized
            },
            edges = form.topology.edges.filter { edge =>
              NodeId.normalize(edge.from) != normalized &&
              NodeId.normalize(edge.to) != normalized
            }
          )
        )
      }
    case Msg.AddEdge(edge) =>
      updateForm(state)(s => s.copy(topology = s.topology.copy(edges = s.topology.edges :+ edge)))
    case Msg.UpdateEdge(edge) =>
      updateForm(state) { form =>
        form.copy(
          topology = form.topology.copy(
            edges = form.topology.edges.map { e =>
              if (e.from == edge.from && e.to == edge.to) then edge else e
            }
          )
        )
      }
    case Msg.RemoveEdge(edge) =>
      updateForm(state) { form =>
        form.copy(
          topology = form.topology.copy(
            edges = form.topology.edges.filterNot(e => e.to == edge.to && e.from == edge.from)
          )
        )
      }

    case Msg.AddShape(shape) =>
      if state.simulation.exists(_.running) then
        state.copy(
          errors = Vector(
            ValidationError(
              "simulation",
              "Cannot edit the topology while the simulation is running"
            )
          )
        )
      else
        state.scenarioForm match
          case None =>
            state.copy(
              errors = Vector(ValidationError("scenarioForm", "No scenario form is open"))
            )

          case Some(form) =>
            addShape(shape, form) match
              case Left(errors) =>
                state.copy(errors = errors.map(ValidationError("topology", _)).toVector)

              case Right(updatedForm) =>
                state.copy(scenarioForm = Some(updatedForm), errors = Vector.empty)

    case Msg.UpdateMalware(malware) =>
      updateForm(state)(s => s.copy(virus = malware))

    case Msg.UpdateAwareness(awareness) =>
      state.scenarioForm match
        case None =>
          state.copy(
            errors = Vector(
              ValidationError("scenarioForm", "No scenario form is open")
            )
          )

        case Some(form) =>
          AwarenessForm.toDomain(awareness) match
            case Left(error) =>
              state.copy(errors = Vector(ValidationError("awareness", error)))

            case Right(value) =>
              state.copy(
                scenarioForm = Some(form.copy(awareness = value)),
                errors = Vector.empty
              )

    case Msg.UpdateCountermeasure(countermeasure) =>
      updateForm(state)(s => s.copy(countermeasureConfig = countermeasure))

  private val shapeNodeCount =
    5 // 5 nodes total for every shape, so the three buttons feel comparable
  private val shapeChannelType = ChannelType.LAN

  /** Generates a small `star` /`ring`/`mesh` shape (`it.unibo.splague.dsl.TopologyShapes`) and
    * merges it into `form` 's topology. Shape ids are namespaced with an incrementing generation
    * suffix (`star1`, `star2`, ...) so pressing the same button more than once never collides with
    * a shape added by an earlier click; a collision with a node the user named by hand is
    * vanishingly unlikely, but still surfaces as an ordinary validation error rather than silently
    * overwriting anything.
    */
  private def addShape(shape: TopologyShape, form: ScenarioForm): ValidationResult[ScenarioForm] =
    val existingIds = form.topology.nodes.map(_.id).toSet
    val nodeType = Node.defaultNodeType

    val shapeResult: ValidationResult[Topology] = shape match
      case TopologyShape.Star =>
        val leafCount = shapeNodeCount - 1
        val base = freshShapeBase("star", existingIds) { b =>
          Set(s"$b-hub") ++ (0 until leafCount).map(i => s"$b-leaf$i")
        }
        topology:
          star(s"$base-hub", nodeType, s"$base-leaf", leafCount, nodeType, shapeChannelType)

      case TopologyShape.Ring =>
        val base = freshShapeBase("ring", existingIds) { b =>
          (0 until shapeNodeCount).map(i => s"$b-$i").toSet
        }
        topology:
          ring(s"$base-", shapeNodeCount, nodeType, shapeChannelType)

      case TopologyShape.Mesh =>
        val base = freshShapeBase("mesh", existingIds) { b =>
          (0 until shapeNodeCount).map(i => s"$b-$i").toSet
        }
        topology:
          mesh(s"$base-", shapeNodeCount, nodeType, shapeChannelType)

    shapeResult.map { shapeTopology =>
      form.copy(
        topology = form.topology.copy(
          nodes = form.topology.nodes ++ shapeTopology.nodes.values.toVector.map(NodeForm.fromNode),
          edges = form.topology.edges ++ shapeTopology.edges.toVector.map(EdgeForm.fromEdge)
        )
      )
    }

  /** The first `"$kind$generation"` (`generation` starting at 1) whose `idsFor` ids don't collide
    * with `existingIds`.
    */
  private def freshShapeBase(kind: String, existingIds: Set[String])(
      idsFor: String => Set[String]
  ): String =
    Iterator
      .from(1)
      .map(generation => s"$kind$generation")
      .find(base => (idsFor(base) & existingIds).isEmpty)
      .get

  private def updateForm(
      state: AppState
  )(change: ScenarioForm => ScenarioForm): AppState =
    state.scenarioForm match
      case Some(form) =>
        state.copy(
          scenarioForm = Some(change(form)),
          errors = Vector.empty
        )

      case None =>
        state.copy(
          errors = Vector(
            ValidationError(
              "scenarioForm",
              "No scenario form is open"
            )
          )
        )
// $COVERAGE-ON$
