package it.unibo.splague.update

import it.unibo.splague.model.node.{Node, NodeType}

/** A rule or condition used to determine if a specific [[Node]] should be isolated as part of a
  * defense mechanism.
  *
  * This class acts as a composable predicate (`Node => Boolean`). Criteria can be chained together
  * using logical [[and]] and [[or]] operators to form complex isolation policies.
  *
  * @param matches
  *   The predicate function that evaluates to `true` if the given node meets the criteria for
  *   isolation, and `false` otherwise.
  */
case class IsolationCriteria(matches: Node => Boolean):

  /** Combines this criterion with another using logical AND.
    *
    * A node will only match the resulting criterion if it satisfies **both** this criterion and the
    * `other` criterion.
    *
    * @param other
    *   The criterion to combine with this one.
    * @return
    *   A new [[IsolationCriteria]] representing the intersection of the two conditions.
    */
  def and(other: IsolationCriteria) = IsolationCriteria(n => matches(n) && other.matches(n))

  /** Combines this criterion with another using logical OR.
    *
    * A node will match the resulting criterion if it satisfies **either** this criterion or the
    * `other` criterion (or both).
    *
    * @param other
    *   The criterion to combine with this one.
    * @return
    *   A new [[IsolationCriteria]] representing the union of the two conditions.
    */
  def or(other: IsolationCriteria) = IsolationCriteria(n => matches(n) || other.matches(n))

object IsolationCriteria:
  /** A default criterion that matches every node. Using this means all nodes are eligible for
    * isolation.
    */
  val all = IsolationCriteria(_ => true)

  /** Creates a criterion that matches nodes based on their [[NodeType]].
    *
    * @param types
    *   The set of node types that should be isolated.
    * @return
    *   An [[IsolationCriteria]] that evaluates to `true` for nodes matching any of the specified
    *   types.
    */
  def byType(types: Set[NodeType]): IsolationCriteria =
    IsolationCriteria(n => types.contains(n.nodeType))

  /** Creates a criterion that matches nodes experiencing a high workload.
    *
    * @param threshold
    *   The minimum workload value (inclusive) required for a node to be isolated.
    * @return
    *   An [[IsolationCriteria]] that evaluates to `true` if the node's workload is >= `threshold`.
    */
  def byMinWorkload(threshold: Double): IsolationCriteria = IsolationCriteria(
    _.workload >= threshold
  )

  /** Creates a criterion that matches vulnerable nodes based on their defense level.
    *
    * @param threshold
    *   The maximum defense level (inclusive) for a node to be considered vulnerable enough for
    *   isolation.
    * @return
    *   An [[IsolationCriteria]] that evaluates to `true` if the node's defense level is <=
    *   `threshold`.
    */
  def byMaxDefense(threshold: Double): IsolationCriteria = IsolationCriteria(
    _.defenseLevel <= threshold
  )
