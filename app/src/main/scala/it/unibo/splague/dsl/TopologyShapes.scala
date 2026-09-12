package it.unibo.splague.dsl

import it.unibo.splague.model.connection.Connection.ChannelType
import it.unibo.splague.model.node.NodeType

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
  */
def star(
    hubId: String,
    hubType: NodeType,
    leafPrefix: String,
    leafCount: Int,
    leafType: NodeType,
    channelType: ChannelType
)(using builder: TopologyBuilder): Unit =
  node(hubId, hubType)
  for i <- 0 until leafCount do
    val leafId = s"$leafPrefix$i"
    node(leafId, leafType)
    hubId <-> leafId via channelType
