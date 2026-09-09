package it.unibo.splague.dsl

import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.model.node.{NodeType, Topology}

def topology(block: TopologyBuilder ?=> Unit): ValidationResult[Topology] =
  given builder: TopologyBuilder = new TopologyBuilder()
  block
  builder.build()

def node(
    id: String,
    nodeType: NodeType,
    patchLevel: Double = 0.0,
    defenseLevel: Double = 0.0,
    workload: Double = 0.0,
    vectors: Set[PropagationVector] = Set(PropagationVector.NetworkExploit)
)(using builder: TopologyBuilder): Unit =
  builder.addNode(id, nodeType, patchLevel, defenseLevel, workload, vectors)
