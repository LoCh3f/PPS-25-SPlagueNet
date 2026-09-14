package it.unibo.splague.model.node

object NodeId:
  opaque type NodeId = String

  /** Normalizes a raw id the same way `of` does, without validating it. Exposed so callers that
    * need to compare or look up ids consistently with declared `NodeId` s — before knowing whether
    * validation will succeed — don't have to duplicate the normalization rule themselves.
    */
  def normalize(rawId: String): String = rawId.trim()

  def of(rawId: String): Either[String, NodeId] =
    val normalized = normalize(rawId)

    if normalized.isEmpty then Left("The ID cannot be empty")
    else if normalized.exists(_.isWhitespace) then Left("The ID cannot contain white space")
    else Right(normalized)

  extension (id: NodeId) def value: String = id
