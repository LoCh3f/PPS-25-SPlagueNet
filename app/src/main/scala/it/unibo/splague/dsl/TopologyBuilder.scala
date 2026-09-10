package it.unibo.splague.dsl

import it.unibo.splague.model.connection.Connection.{Channel, ChannelType, Edge}
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

private final case class EdgeSpec(
    sourceId: String,
    targetId: String,
    channelType: ChannelType,
    bandwidth: Option[Double],
    latency: Option[Double],
    jitter: Option[Double],
    packetLoss: Option[Double]
)

/** Mutable accumulator backing the `topology { ... }` DSL block. Not part of the public API — only
  * reachable via a `given` instance inside the block, so all mutation stays contained behind the
  * pure `topology(...)` entry point.
  */
private final class TopologyBuilder:

  private val nodeSpecs = mutable.ArrayBuffer.empty[NodeSpec]
  private val edgeSpecs = mutable.ArrayBuffer.empty[EdgeSpec]

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

  def addEdge(
      sourceId: String,
      targetId: String,
      channelType: ChannelType,
      bandwidth: Option[Double],
      latency: Option[Double],
      jitter: Option[Double],
      packetLoss: Option[Double]
  ): Unit =
    edgeSpecs += EdgeSpec(sourceId, targetId, channelType, bandwidth, latency, jitter, packetLoss)

  /** Validates every declared node spec and assembles a `Topology`.
    *
    * Validation errors are accumulated rather than short-circuited: a malformed ID does not prevent
    * other errors (e.g. duplicate IDs among the remaining valid specs) from also being reported.
    * Duplicate-ID checks run only over specs that already passed ID validation, so a malformed ID
    * is never double-counted as a spurious duplicate.
    *
    * Edge resolution currently assumes every declared edge refers to nodes that exist and were
    * declared exactly once; self-loop, duplicate-edge, and unknown-node-reference validation are
    * not yet implemented and will be added as dedicated TDD cycles.
    *
    * @return
    *   `Right(topology)`, or `Left(errors)` with every validation failure found.
    */
  def build(): ValidationResult[Topology] =
    val nodeResults: List[ValidationResult[Node]] =
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

    val nodeValidationErrors = nodeResults.collect { case Left(e) => e }.flatten
    val validNodes = nodeResults.collect { case Right(node) => node }

    val duplicateNodeErrors = validNodes
      .groupBy(_.nodeId.value)
      .collect { case (id, nodes) if nodes.sizeIs > 1 => s"Duplicate node id: $id" }
      .toList

    val nodesById = validNodes.map(node => node.nodeId.value -> node).toMap

    val edges = edgeSpecs.toList.map { spec =>
      val channel = Channel.default(
        spec.channelType,
        spec.bandwidth,
        spec.latency,
        spec.jitter,
        spec.packetLoss
      )
      Edge(nodesById(spec.sourceId), nodesById(spec.targetId), channel, None)
    }.toSet

    val errors = nodeValidationErrors ++ duplicateNodeErrors

    if errors.nonEmpty then Left(errors)
    else Right(Topology(nodesById, edges))
