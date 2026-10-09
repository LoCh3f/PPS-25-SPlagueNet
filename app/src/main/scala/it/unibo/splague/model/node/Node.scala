package it.unibo.splague.model.node

import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.model.node.NodeId.NodeId

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

case class Node private[splague] (
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
  val defaultDefenseLevel: Double = 0.0
  val defaultState: NodeState = NodeState.Healthy
  val defaultWorkload: Double = 0.0
  val defaultVectors: Set[PropagationVector] = Set.empty

  private def fieldIsInRange(field: Double, range: (Double, Double)): Either[String, Double] =
    val (lower, upper) = range
    Either.cond(
      field >= lower && field <= upper,
      field,
      s"Field value $field must be in range: [$lower, $upper]"
    )

  def test(
      nodeId: NodeId,
      nodeType: NodeType = defaultNodeType,
      patchLevel: Double = defaultPatchLevel,
      defenseLevel: Double = defaultDefenseLevel,
      state: NodeState = defaultState,
      workload: Double = defaultWorkload,
      vectors: Set[PropagationVector] = defaultVectors
  ): Either[String, Node] =
    Node(nodeId, nodeType, patchLevel, defenseLevel, state, workload, vectors)

  def apply(
      nodeId: NodeId,
      nodeType: NodeType = defaultNodeType,
      patchLevel: Double = defaultPatchLevel,
      defenseLevel: Double = defaultDefenseLevel,
      state: NodeState = defaultState,
      workload: Double = defaultWorkload,
      vectors: Set[PropagationVector] = defaultVectors
  ): Either[String, Node] =
    for
      validPatchLevel <- fieldIsInRange(patchLevel, (0.0, 1.0))
      validDefenseLevel <- fieldIsInRange(defenseLevel, (0.0, 1.0))
    yield new Node(nodeId, nodeType, validPatchLevel, validDefenseLevel, state, workload, vectors)
