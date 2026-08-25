package it.unibo.splague.model.node

import it.unibo.splague.model.connection.Connection.Edge
import it.unibo.splague.model.node.NodeId.NodeId
import it.unibo.splague.model.node.{Node, NodeId}
import sun.jvm.hotspot.HelloWorld.e

sealed trait NodeType:
  def detectionCoefficient: Double
  def structuralVulnerability: Double

object NodeType:
  case object IoTDevice extends NodeType:
    def detectionCoefficient = 0.3
    def structuralVulnerability = 1.3

  case object Workstation extends NodeType:
    def detectionCoefficient = 1.5
    def structuralVulnerability = 0.8

  case object Router extends NodeType:
    def detectionCoefficient = 0.3
    def structuralVulnerability = 1.3

  case object Server extends NodeType:
    def detectionCoefficient = 0.3
    def structuralVulnerability = 1.3

  case object MobileDevice extends NodeType:
    def detectionCoefficient = 0.3
    def structuralVulnerability = 1.3

enum NodeState:
  case Healthy, Infected, Quarantined, Immune, Destroyed

case class Node(
    nodeId: NodeId,
    nodeType: NodeType,
    patchLevel: Double,
    defenseLevel: Double,
    state: NodeState,
    workload: Double
)
