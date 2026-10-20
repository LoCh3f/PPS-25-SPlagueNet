package it.unibo.splague.model.node

import it.unibo.splague.utils.config.UIConfig.ErrorMessages

object NodeId:
  opaque type NodeId = String

  /** Normalizes a raw id the same way `of` does, without validating it. Exposed so callers that
    * need to compare or look up ids consistently with declared `NodeId` s — before knowing whether
    * validation will succeed — don't have to duplicate the normalization rule themselves.
    */
  def normalize(rawId: String): String = rawId.trim()

  def of(rawId: String): Either[String, NodeId] =
    val normalized = normalize(rawId)

    if normalized.isEmpty then Left(ErrorMessages.EMPTY_NODE_ID)
    else if normalized.exists(_.isWhitespace) then Left(ErrorMessages.NODE_ID_WHITESPACE)
    else Right(normalized)

  extension (id: NodeId) def value: String = id
