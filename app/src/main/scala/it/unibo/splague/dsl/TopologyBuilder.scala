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

  private def resolveNodes(): (List[String], Map[String, Node]) =
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

    val (validationErrors, validNodes) = ValidationResult.partition(nodeResults)

    val duplicateErrors = validNodes
      .groupBy(_.nodeId.value)
      .collect { case (id, nodes) if nodes.sizeIs > 1 => s"Duplicate node id: $id" }
      .toList

    (validationErrors ++ duplicateErrors, validNodes.map(node => node.nodeId.value -> node).toMap)

  /** Resolves each declared edge against `nodesById`. Self-loops and references to unknown node ids
    * are reported as errors rather than thrown. Edges are also checked for duplicates against an
    * *unordered* key (`Set(sourceId, targetId)`), since the network model treats connections as
    * undirected: `"A" <-> "B"` and `"B" <-> "A"` describe the same edge, and declaring either twice
    * is an error rather than a silent no-op.
    */
  private def resolveEdges(nodesById: Map[String, Node]): (List[String], Set[Edge]) =
    case class ResolvedEdge(key: Set[String], edge: Edge)

    val edgeResults: List[ValidationResult[ResolvedEdge]] =
      edgeSpecs.toList.map { spec =>
        if spec.sourceId == spec.targetId then
          Left(List(s"Self-loop edges are not allowed: ${spec.sourceId}"))
        else
          (nodesById.get(spec.sourceId), nodesById.get(spec.targetId)) match
            case (Some(source), Some(target)) =>
              val channel = Channel.default(
                spec.channelType,
                spec.bandwidth,
                spec.latency,
                spec.jitter,
                spec.packetLoss
              )
              Right(
                ResolvedEdge(Set(spec.sourceId, spec.targetId), Edge(source, target, channel, None))
              )
            case (sourceOpt, targetOpt) =>
              val missingSource = Option.unless(sourceOpt.isDefined)(spec.sourceId)
              val missingTarget = Option.unless(targetOpt.isDefined)(spec.targetId)
              Left(
                (missingSource ++ missingTarget).toList
                  .map(id => s"Edge references unknown node id: $id")
              )
      }

    val (referenceErrors, resolvedEdges) = ValidationResult.partition(edgeResults)

    val duplicateErrors = resolvedEdges
      .groupBy(_.key)
      .collect {
        case (key, edges) if edges.sizeIs > 1 =>
          s"Duplicate edge between ${key.toList.sorted.mkString(" and ")}"
      }
      .toList

    (referenceErrors ++ duplicateErrors, resolvedEdges.map(_.edge).toSet)

  /** Validates every declared node and edge spec and assembles a `Topology`.
    *
    * Errors are accumulated rather than short-circuited across both nodes and edges: a malformed
    * node ID does not prevent duplicate-ID errors, self-loop errors, unknown-reference errors, or
    * duplicate-edge errors from also being reported in the same result.
    *
    * @return
    *   `Right(topology)`, or `Left(errors)` with every validation failure found.
    */
  def build(): ValidationResult[Topology] =
    val (nodeErrors, nodesById) = resolveNodes()
    val (edgeErrors, edges) = resolveEdges(nodesById)

    val errors = nodeErrors ++ edgeErrors

    if errors.nonEmpty then Left(errors)
    else Right(Topology(nodesById, edges))
