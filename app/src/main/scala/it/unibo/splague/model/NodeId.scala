package it.unibo.splague.model

object NodeId:
  opaque type NodeId = String

  def of(rawId: String): Either[String, NodeId] =
    val normalized = rawId.trim()

    if normalized.isEmpty then Left("The ID cannot be empty")
    else if normalized.exists(_.isWhitespace) then Left("The ID cannot contain white space")
    else Right(normalized)

  extension (id: NodeId) def value: String = id
