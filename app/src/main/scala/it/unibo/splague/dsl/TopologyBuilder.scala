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

    val errors = results.collect { case Left(e) => e }.flatten

    if errors.nonEmpty then Left(errors)
    else
      val nodes = results.collect { case Right(node) => node }
      Right(
        Topology(
          nodes.map(node => node.nodeId.value -> node).toMap,
          Set.empty
        )
      )
