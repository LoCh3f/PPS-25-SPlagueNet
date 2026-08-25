package it.unibo.splague.simulation

import it.unibo.splague.model.{Node, Topology}
import it.unibo.splague.model.malware.Malware

@SerialVersionUID(1L)
final case class Scenario private (
    name: String,
    topology: Topology,
    virus: Malware,
    startingNode: Node,
    tick: Int,
    seed: Int,
    maxIterations: Int
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
      maxIterations: Int
  ): Either[String, Scenario] =
    for
      validName <- validateName(name)
      validStartingNode <- validateStartingNode(startingNode, topology)
      validMaxIter <- validateMaxIterations(maxIterations)
    yield new Scenario(validName, topology, virus, validStartingNode, tick, seed, validMaxIter)
