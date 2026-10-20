package it.unibo.splague.model.node

import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.model.node.NodeId.NodeId
import it.unibo.splague.utils.config.SimulationConfig.{Bounds, Detection, Vulnerability}

sealed trait NodeType:
  def detectionCoefficient: Double
  def structuralVulnerability: Double

object NodeType:
  case object Workstation extends NodeType:
    def detectionCoefficient = Detection.WORKSTATION_COEFFICIENT
    def structuralVulnerability = Vulnerability.WORKSTATION_FACTOR

  case object Server extends NodeType:
    def detectionCoefficient = Detection.SERVER_COEFFICIENT // better monitoring(log/SIEM)
    def structuralVulnerability = Vulnerability.SERVER_FACTOR // typical hardening

  case object Router extends NodeType:
    def detectionCoefficient = Detection.ROUTER_COEFFICIENT
    def structuralVulnerability = Vulnerability.ROUTER_FACTOR

  case object IoTDevice extends NodeType:
    def detectionCoefficient = Detection.IOT_DEVICE_COEFFICIENT // logging absent
    def structuralVulnerability =
      Vulnerability.IOT_DEVICE_FACTOR // intrinsic vulnerability of the firmware

  case object MobileDevice extends NodeType:
    def detectionCoefficient = Detection.MOBILE_DEVICE_COEFFICIENT
    def structuralVulnerability = Vulnerability.MOBILE_DEVICE_FACTOR

enum NodeState:
  case Healthy, Infected, Quarantined, Immune, Destroyed

case class Node private (
    nodeId: NodeId,
    nodeType: NodeType,
    patchLevel: Double,
    defenseLevel: Double,
    state: NodeState,
    workload: Double,
    vectors: Set[PropagationVector]
):

  def copy(
      nodeId: NodeId = this.nodeId,
      nodeType: NodeType = this.nodeType,
      patchLevel: Double = this.patchLevel,
      defenseLevel: Double = this.defenseLevel,
      state: NodeState = this.state,
      workload: Double = this.workload,
      vectors: Set[PropagationVector] = this.vectors
  ): Node =
    new Node(nodeId, nodeType, patchLevel, defenseLevel, state, workload, vectors)

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
      validPatchLevel <- fieldIsInRange(
        patchLevel,
        (Bounds.MIN_PROBABILITY, Bounds.MAX_PROBABILITY)
      )
      validDefenseLevel <- fieldIsInRange(
        defenseLevel,
        (Bounds.MIN_PROBABILITY, Bounds.MAX_PROBABILITY)
      )
    yield new Node(nodeId, nodeType, validPatchLevel, validDefenseLevel, state, workload, vectors)
