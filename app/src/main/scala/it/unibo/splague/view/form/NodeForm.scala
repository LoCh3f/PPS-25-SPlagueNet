package it.unibo.splague.view.form

import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType}

final case class NodeForm(
    id: String,
    nodeType: NodeType,
    patchLevel: String,
    defenseLevel: String,
    state: NodeState,
    workload: String,
    vectors: Set[PropagationVector]
)

object NodeForm:

  def fromNode(node: Node): NodeForm =
    NodeForm(
      id = node.nodeId.value,
      nodeType = node.nodeType,
      patchLevel = node.patchLevel.toString,
      defenseLevel = node.defenseLevel.toString,
      state = node.state,
      workload = node.workload.toString,
      vectors = node.vectors
    )

  def toDomain(form: NodeForm): Either[String, Node] =
    for
      nodeId <- NodeId.of(form.id)
      patchLevel <- parseDouble(form.patchLevel, "patchLevel")
      defense <- parseDouble(form.defenseLevel, "defenseLevel")
      workload <- parseDouble(form.workload, "workload")
    yield Node(
      nodeId = nodeId,
      nodeType = form.nodeType,
      patchLevel = patchLevel,
      defenseLevel = defense,
      state = form.state,
      workload = workload,
      vectors = form.vectors
    )

  private def parseDouble(s: String, field: String): Either[String, Double] =
    s.trim.toDoubleOption.toRight(s"$field must be a number, got '$s'")
