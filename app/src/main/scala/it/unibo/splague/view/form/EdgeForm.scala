package it.unibo.splague.view.form

import it.unibo.splague.model.connection.Connection.Edge
import it.unibo.splague.model.connection.Protocol.ApplicationProtocol
import it.unibo.splague.model.node.{Node, NodeId}
import it.unibo.splague.model.node.NodeId.*

final case class EdgeForm(
    from: String,
    to: String,
    channel: ChannelForm,
    protocol: Option[ApplicationProtocol]
)

object EdgeForm:

  def fromEdge(edge: Edge): EdgeForm =
    EdgeForm(
      from = edge.source.nodeId.value,
      to = edge.target.nodeId.value,
      channel = ChannelForm.fromChannel(edge.channel),
      protocol = edge.protocol
    )

  def toDomain(
      form: EdgeForm,
      nodesById: Map[String, Node]
  ): Either[String, Edge] =
    for
      sourceId <- NodeId.of(form.from)
      targetId <- NodeId.of(form.to)

      source <- nodesById
        .get(sourceId.value)
        .toRight(s"Unknown source node '${form.from}'")

      target <- nodesById
        .get(targetId.value)
        .toRight(s"Unknown target node '${form.to}'")

      channel <- ChannelForm.toDomain(form.channel)
    yield Edge(
      source = source,
      target = target,
      channel = channel,
      protocol = form.protocol
    )
