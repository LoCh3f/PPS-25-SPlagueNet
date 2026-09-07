package it.unibo.splague.simulation

import it.unibo.splague.model.Awareness
import it.unibo.splague.model.countermeasures.CountermeasureConfig
import it.unibo.splague.model.node.{Node, Topology}
import it.unibo.splague.model.malware.Malware

final case class Scenario(
    name: String,
    topology: Topology,
    virus: Malware,
    startingNode: Node,
    tick: Int,
    seed: Int,
    maxIterations: Int,
    awareness: Awareness,
    countermeasureConfig: CountermeasureConfig
)

object Scenario:
  private def validateName(name: String): Either[String, String] =
    val trimmed = name.trim
    Either.cond(trimmed.nonEmpty, trimmed, "The scenario name can't be empty")

  private def validateStartingNode(node: Node, t: Topology): Either[String, Node] =
    Either.cond(
      t.nodes.values.toSet.contains(node),
      node,
      "The starting node is not part of the topology"
    )

  private def validateMaxIterations(iterations: Int): Either[String, Int] =
    Either.cond(
      iterations > 0,
      iterations,
      "The number of maximum iterations must be positive and greater than 0"
    )

  def apply(
      name: String,
      topology: Topology,
      virus: Malware,
      startingNode: Node,
      tick: Int,
      seed: Int,
      maxIterations: Int,
      countermeasureConfig: CountermeasureConfig = CountermeasureConfig.empty
  ): Either[String, Scenario] =
    for
      validName <- validateName(name)
      validStartingNode <- validateStartingNode(startingNode, topology)
      validMaxIter <- validateMaxIterations(maxIterations)
    yield new Scenario(
      validName,
      topology,
      virus,
      validStartingNode,
      tick,
      seed,
      validMaxIter,
      awareness = Awareness.none,
      countermeasureConfig
    )
