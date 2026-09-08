package it.unibo.splague.model.node

import it.unibo.splague.model.connection.Connection.Edge
import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.model.node.NodeId.NodeId
import it.unibo.splague.model.node.{Node, NodeId}
import sun.jvm.hotspot.HelloWorld.e

sealed trait NodeType:
  def detectionCoefficient: Double
  def structuralVulnerability: Double

object NodeType:
  case object Workstation extends NodeType:
    def detectionCoefficient = 1.0
    def structuralVulnerability = 1.0

  case object Server extends NodeType:
    def detectionCoefficient = 1.5 // better monitoring(log/SIEM)
    def structuralVulnerability = 0.8 // typical hardening

  case object Router extends NodeType:
    def detectionCoefficient = 0.8
    def structuralVulnerability = 1.0

  case object IoTDevice extends NodeType:
    def detectionCoefficient = 0.3 // logging absent
    def structuralVulnerability = 1.3 // intrinsic vulnerability of the firmware

  case object MobileDevice extends NodeType:
    def detectionCoefficient = 0.9
    def structuralVulnerability = 1.0

enum NodeState:
  case Healthy, Infected, Quarantined, Immune, Destroyed

case class Node(
    nodeId: NodeId,
    nodeType: NodeType,
    patchLevel: Double,
    defenseLevel: Double,
    state: NodeState,
    workload: Double,
    vectors: Set[PropagationVector]
)
object Node:
  val defaultNodeType: NodeType = NodeType.Server
  val defaultPatchLevel: Double = 0.0
  val defaultDefenseLevel: Double = 1.0
  val defaultState: NodeState = NodeState.Healthy
  val defaultWorkload: Double = 0.0
  val defaultVectors: Set[PropagationVector] = Set.empty

  def test(
      nodeId: NodeId,
      nodeType: NodeType = defaultNodeType,
      patchLevel: Double = defaultPatchLevel,
      defenseLevel: Double = defaultDefenseLevel,
      state: NodeState = defaultState,
      workload: Double = defaultWorkload,
      vectors: Set[PropagationVector] = defaultVectors
  ): Node =
    Node(nodeId, nodeType, patchLevel, defenseLevel, state, workload, vectors)
