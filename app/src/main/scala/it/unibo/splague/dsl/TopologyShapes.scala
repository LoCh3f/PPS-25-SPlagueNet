package it.unibo.splague.dsl

import it.unibo.splague.model.connection.Connection.ChannelType
import it.unibo.splague.model.node.NodeType

/** Declares `count` nodes named `"${prefix}0"` through `"${prefix}(count - 1)"`, all of `nodeType`.
  * Shared by `ring` and `mesh`, which differ only in which edges they declare afterward.
  *
  * @return
  *   the declared ids, in order
  */
private def declareIndexedNodes(
    prefix: String,
    count: Int,
    nodeType: NodeType
)(using builder: TopologyBuilder): List[String] =
  val ids = (0 until count).map(i => s"$prefix$i").toList
  for id <- ids do node(id, nodeType)
  ids

/** Declares a star-shaped topology inside an enclosing `topology { ... }` block: one central hub
  * node, connected individually to `leafCount` leaf nodes. No edges exist between leaves.
  *
  * Built entirely from the existing `node` /`<->`/`via` DSL primitives, so every validation check
  * they already perform (malformed ids, duplicate ids, duplicate edges) still applies to the nodes
  * and edges this generates — a `star` call participates in the same accumulated `ValidationResult`
  * as any handwritten declaration in the same block.
  *
  * @param hubId
  *   id of the central node
  * @param hubType
  *   node type of the hub
  * @param leafPrefix
  *   prefix used to generate each leaf's id; leaves are named `"${leafPrefix}0"`,
  *   `"${leafPrefix}1"`, ... up to `leafCount - 1`
  * @param leafCount
  *   number of leaf nodes to declare; `0` declares only the hub, with no edges
  * @param leafType
  *   node type shared by every leaf
  * @param channelType
  *   channel type used for every hub-to-leaf edge
  * @return
  *   the hub's id, so it can be referenced to wire this shape to other declarations in the same
  *   `topology { ... }` block
  */
def star(
    hubId: String,
    hubType: NodeType,
    leafPrefix: String,
    leafCount: Int,
    leafType: NodeType,
    channelType: ChannelType
)(using builder: TopologyBuilder): String =
  node(hubId, hubType)
  for i <- 0 until leafCount do
    val leafId = s"$leafPrefix$i"
    node(leafId, leafType)
    hubId <-> leafId via channelType
  hubId

/** Declares a ring-shaped topology: `count` nodes, each connected to exactly its two neighbors in a
  * cycle (`node0 <-> node1 <-> ... <-> node(count-1) <-> node0`).
  *
  * Two counts need special handling to avoid the generator producing an edge declaration this same
  * DSL would reject: `count == 1` would otherwise connect a node to itself (a self-loop), and
  * `count == 2` would otherwise declare the same undirected edge twice, once in each direction (a
  * duplicate edge) — both are avoided explicitly rather than relying on the caller to notice the
  * resulting validation error.
  *
  * @param prefix
  *   prefix used to generate each node's id, `"${prefix}0"` through `"${prefix}(count - 1)"`
  * @param count
  *   number of nodes in the ring; `0` declares nothing, `1` declares a single node with no edges
  * @param nodeType
  *   node type shared by every node in the ring
  * @param channelType
  *   channel type used for every edge in the ring
  * @return
  *   the ids of every node in the ring, in order, so it can be wired to other declarations in the
  *   same `topology { ... }` block
  */
def ring(
    prefix: String,
    count: Int,
    nodeType: NodeType,
    channelType: ChannelType
)(using builder: TopologyBuilder): List[String] =
  val ids = declareIndexedNodes(prefix, count, nodeType)

  count match
    case n if n <= 1 => () // no edges: nothing to connect, or would self-loop
    case 2           => s"${prefix}0" <-> s"${prefix}1" via channelType
    case n =>
      for i <- 0 until n do
        val a = s"$prefix$i"
        val b = s"$prefix${(i + 1) % n}"
        a <-> b via channelType

  ids

/** Declares a fully-connected (complete-graph) topology: `count` nodes, with an edge between every
  * distinct pair. Iterating pairs as `i < j` naturally avoids both self-loops (`i == j` never
  * occurs) and duplicate edges (each unordered pair is visited exactly once), unlike `ring`, so no
  * special-casing is needed here.
  *
  * @param prefix
  *   prefix used to generate each node's id, `"${prefix}0"` through `"${prefix}(count - 1)"`
  * @param count
  *   number of nodes in the mesh; `0` or `1` declares no edges
  * @param nodeType
  *   node type shared by every node in the mesh
  * @param channelType
  *   channel type used for every edge in the mesh
  * @return
  *   the ids of every node in the mesh, in order, so it can be wired to other declarations in the
  *   same `topology { ... }` block
  */
def mesh(
    prefix: String,
    count: Int,
    nodeType: NodeType,
    channelType: ChannelType
)(using builder: TopologyBuilder): List[String] =
  val ids = declareIndexedNodes(prefix, count, nodeType)

  for
    i <- 0 until count
    j <- (i + 1) until count
  do s"$prefix$i" <-> s"$prefix$j" via channelType

  ids
