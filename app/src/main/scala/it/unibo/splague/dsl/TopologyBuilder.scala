package it.unibo.splague.dsl

import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}

import scala.collection.mutable

private final case class NodeSpec(
    id: String,
    nodeType: NodeType,
    patchLevel: Double,
    defenseLevel: Double,
    workload: Double,
    vectors: Set[PropagationVector]
)

/** Mutable accumulator backing the `topology { ... }` DSL block. Not part of the public API — only
  * reachable via a `given` instance inside the block, so all mutation stays contained behind the
  * pure `topology(...)` entry point.
  */
private final class TopologyBuilder:

  private val nodeSpecs = mutable.ArrayBuffer.empty[NodeSpec]

  def addNode(
      id: String,
      nodeType: NodeType,
      patchLevel: Double,
      defenseLevel: Double,
      workload: Double,
      vectors: Set[PropagationVector]
  ): Unit =
    nodeSpecs += NodeSpec(
      id = id,
      nodeType = nodeType,
      patchLevel = patchLevel,
      defenseLevel = defenseLevel,
      workload = workload,
      vectors = vectors.toSet
    )

  /** Validates every declared node spec and assembles a `Topology`.
    *
    * Validation errors are accumulated rather than short-circuited: a malformed ID does not prevent
    * other errors (e.g. duplicate IDs among the remaining valid specs) from also being reported.
    * Duplicate-ID checks run only over specs that already passed ID validation, so a malformed ID
    * is never double-counted as a spurious duplicate.
    *
    * @return
    *   `Right(topology)` with no edges yet (edge support lands separately), or `Left(errors)` with
    *   every validation failure found.
    */
  def build(): ValidationResult[Topology] =
    val results: List[ValidationResult[Node]] =
      nodeSpecs.toList.map { spec =>
        NodeId
          .of(spec.id)
          .left
          .map(List(_))
          .map { id =>
            Node(
              id,
              spec.nodeType,
              spec.patchLevel,
              spec.defenseLevel,
              NodeState.Healthy,
              spec.workload,
              spec.vectors
            )
          }
      }

    val validationErrors = results.collect { case Left(e) => e }.flatten
    val validNodes = results.collect { case Right(node) => node }

    val duplicateErrors = validNodes
      .groupBy(_.nodeId.value)
      .collect { case (id, nodes) if nodes.sizeIs > 1 => s"Duplicate node id: $id" }
      .toList

    val errors = validationErrors ++ duplicateErrors

    if errors.nonEmpty then Left(errors)
    else Right(Topology(validNodes.map(node => node.nodeId.value -> node).toMap, Set.empty))
