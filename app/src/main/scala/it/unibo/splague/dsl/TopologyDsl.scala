package it.unibo.splague.dsl

import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.model.node.{NodeType, Topology}

/** Entry point for declaratively building a `Topology`.
  *
  * Usage:
  * {{{
  * val result: ValidationResult[Topology] = topology:
  *   node("A", Workstation)
  *   node("B", Server, defenseLevel = 0.3)
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
