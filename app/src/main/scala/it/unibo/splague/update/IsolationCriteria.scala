package it.unibo.splague.update

import it.unibo.splague.model.node.{Node, NodeType}

case class IsolationCriteria(matches: Node => Boolean):
  def and(other: IsolationCriteria) = IsolationCriteria(n => matches(n) && other.matches(n))
  def or(other: IsolationCriteria) = IsolationCriteria(n => matches(n) || other.matches(n))

object IsolationCriteria:
  val all = IsolationCriteria(_ => true)
  def byType(types: Set[NodeType]): IsolationCriteria =
    IsolationCriteria(n => types.contains(n.nodeType))
  def byMinWorkload(threshold: Double): IsolationCriteria = IsolationCriteria(
    _.workload >= threshold
  )
  def byMaxDefense(threshold: Double): IsolationCriteria = IsolationCriteria(
    _.defenseLevel <= threshold
  )
