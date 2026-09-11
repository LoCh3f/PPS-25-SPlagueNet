package it.unibo.splague.dsl

import it.unibo.splague.model.connection.Connection.ChannelType
import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.model.node.{NodeType, Topology}

import scala.annotation.targetName

/** Entry point for declaratively building a `Topology`.
  *
  * Usage:
  * {{{
  * val result: ValidationResult[Topology] = topology:
  *   node("A", Workstation)
  *   node("B", Server, defenseLevel = 0.3)
  *   "A" <-> "B" via LAN
  * }}}
  *
  * The block runs against a fresh, block-scoped `TopologyBuilder` (threaded implicitly via context
  * function syntax), so nothing outside this function can observe or reuse builder state.
  */
def topology(block: TopologyBuilder ?=> Unit): ValidationResult[Topology] =
  given builder: TopologyBuilder = new TopologyBuilder()
  block
  builder.build()

/** Declares a node in an enclosing `topology { ... }` block.
  *
  * @param id
  *   raw node identifier; validated via `NodeId.of` at build time (non-empty, no whitespace)
  * @param vectors
  *   propagation vectors this node accepts; defaults to `NetworkExploit` for convenience
  */
def node(
    id: String,
    nodeType: NodeType,
    patchLevel: Double = 0.0,
    defenseLevel: Double = 0.0,
    workload: Double = 0.0,
    vectors: Set[PropagationVector] = Set(PropagationVector.NetworkExploit)
)(using builder: TopologyBuilder): Unit =
  builder.addNode(id, nodeType, patchLevel, defenseLevel, workload, vectors)

extension (id: String)
  /** Declares a pending, undirected connection to `other`, to be completed with `via`. */
  @targetName("connectedTo")
  infix def <->(other: String): PendingEdge = PendingEdge(id, other)

/** Optional channel-parameter values used to override the defaults associated with a
  * [[ChannelType]].
  *
  * A parameter set to `None` keeps the default value defined for the selected channel type. A
  * parameter set to `Some(value)` replaces that default.
  *
  * This type is used by [[PendingEdge.via]] so that channel parameters can be supplied while
  * preserving the infix DSL syntax:
  *
  * {{{
  * "A" <-> "B" via LAN
  *
  * "A" <-> "B" via (
  *   WAN,
  *   ChannelOverrides(bandwidth = Some(50.0))
  * )
  * }}}
  *
  * @param bandwidth
  *   optional bandwidth override
  * @param latency
  *   optional latency override
  * @param jitter
  *   optional jitter override
  * @param packetLoss
  *   optional packet-loss override
  */
final case class ChannelOverrides(
    bandwidth: Option[Double] = None,
    latency: Option[Double] = None,
    jitter: Option[Double] = None,
    packetLoss: Option[Double] = None
)

object ChannelOverrides:
  val none: ChannelOverrides = ChannelOverrides()

/** Intermediate value produced by `<->`; completed with `via` to declare the edge's channel. */
final case class PendingEdge(idA: String, idB: String):
  /** Completes a pending edge with a channel type, and optional overrides for its parameters.
    * Unspecified parameters fall back to `Channel.default` for the given `channelType`.
    */
  infix def via(
      channelType: ChannelType,
      overrides: ChannelOverrides = ChannelOverrides.none
  )(using builder: TopologyBuilder): Unit =
    builder.addEdge(
      idA,
      idB,
      channelType,
      overrides.bandwidth,
      overrides.latency,
      overrides.jitter,
      overrides.packetLoss
    )
