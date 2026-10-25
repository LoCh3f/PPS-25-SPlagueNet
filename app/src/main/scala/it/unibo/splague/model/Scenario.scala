package it.unibo.splague.model

import it.unibo.splague.model.Awareness
import it.unibo.splague.model.countermeasures.CountermeasureConfig
import it.unibo.splague.model.malware.Malware
import it.unibo.splague.model.node.{Node, Topology}
import it.unibo.splague.utils.config.UIConfig.ErrorMessages

final case class Scenario private (
    name: String,
    topology: Topology,
    virus: Malware,
    startingNode: Node,
    tick: Int,
    seed: Int,
    maxIterations: Int,
    awareness: Awareness,
    countermeasureConfig: CountermeasureConfig,
    baselineWorkload: Map[String, Double]
):
  def copy(
      name: String = this.name,
      topology: Topology = this.topology,
      virus: Malware = this.virus,
      startingNode: Node = this.startingNode,
      tick: Int = this.tick,
      seed: Int = this.seed,
      maxIterations: Int = this.maxIterations,
      awareness: Awareness = this.awareness,
      countermeasureConfig: CountermeasureConfig = this.countermeasureConfig,
      baselineWorkload: Map[String, Double] = this.baselineWorkload
  ) = new Scenario(
    name,
    topology,
    virus,
    startingNode,
    tick,
    seed,
    maxIterations,
    awareness,
    countermeasureConfig,
    baselineWorkload
  )

object Scenario:
  private def validateName(name: String): Either[String, String] =
    val trimmed = name.trim
    Either.cond(trimmed.nonEmpty, trimmed, ErrorMessages.EMPTY_SCENARIO_NAME)

  private def validateStartingNode(node: Node, t: Topology): Either[String, Node] =
    Either.cond(
      t.nodes.values.toSet.contains(node),
      node,
      ErrorMessages.NODE_NOT_IN_TOPOLOGY
    )

  private def validateMaxIterations(iterations: Int): Either[String, Int] =
    Either.cond(
      iterations > 0,
      iterations,
      ErrorMessages.INVALID_ITERATION_COUNT
    )

  def apply(
      name: String,
      topology: Topology,
      virus: Malware,
      startingNode: Node,
      tick: Int,
      seed: Int,
      maxIterations: Int,
      countermeasureConfig: CountermeasureConfig = CountermeasureConfig.empty,
      awareness: Awareness = Awareness.none
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
      awareness,
      countermeasureConfig,
      baselineWorkload = topology.nodes.map((id, node) => id -> node.workload)
    )
