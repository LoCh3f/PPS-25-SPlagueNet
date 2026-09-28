package it.unibo.splague.dsl

import it.unibo.splague.model.countermeasures.CountermeasureConfig
import it.unibo.splague.model.{Probability, Scenario}
import it.unibo.splague.model.malware.{
  Malware,
  MalwareKind,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.{Node, NodeId, Topology}
import it.unibo.splague.utils.config.SimulationConfig

import scala.collection.mutable

private final case class MalwareSpec(
    name: String,
    kind: MalwareKind,
    infectivity: Double,
    stealth: Double,
    severity: PayloadSeverityLevel,
    persistence: Double,
    footprint: Double,
    vectors: Set[PropagationVector]
)

/** Mutable accumulator backing the `scenario { ... }` DSL block. Not part of the public API — only
  * reachable via a `given` instance inside the block, like `TopologyBuilder`.
  *
  * Every declaration is recorded as-is (a repeated one is kept too, so it can be reported), and
  * everything is validated together in `build`.
  */
private final class ScenarioBuilder(name: String):

  private val networks = mutable.ArrayBuffer.empty[ValidationResult[Topology]]
  private val malwares = mutable.ArrayBuffer.empty[MalwareSpec]
  private val counterConfig = mutable.ArrayBuffer.empty[ValidationResult[CountermeasureConfig]]
  private val startingNodeIds = mutable.ArrayBuffer.empty[String]
  private val seeds = mutable.ArrayBuffer.empty[Int]
  private val maxIterationValues = mutable.ArrayBuffer.empty[Int]

  def setNetwork(result: ValidationResult[Topology]): Unit = networks += result
  def setMalware(spec: MalwareSpec): Unit = malwares += spec
  def setCountermeasures(result: ValidationResult[CountermeasureConfig]): Unit =
    counterConfig += result
  def setStartingNode(id: String): Unit = startingNodeIds += id
  def setSeed(value: Int): Unit = seeds += value
  def setMaxIterations(value: Int): Unit = maxIterationValues += value

  /** Exactly one declaration is required: none and more than one are both errors. */
  private def single[A](
      declared: List[A],
      missing: String,
      duplicated: String
  ): ValidationResult[A] =
    declared match
      case only :: Nil => Right(only)
      case Nil         => Left(List(missing))
      case _           => Left(List(duplicated))

  /** At most one declaration is allowed: none means "use the default". */
  private def atMostOne[A](
      declared: List[A],
      duplicated: String
  ): ValidationResult[Option[A]] =
    declared match
      case Nil         => Right(None)
      case only :: Nil => Right(Some(only))
      case _           => Left(List(duplicated))

  private def errorsOf(results: ValidationResult[?]*): List[String] =
    results.toList.flatMap(_.swap.getOrElse(Nil))

  private def probability(field: String, value: Double): ValidationResult[Probability] =
    Probability(value).left.map(error => List(s"$field: $error"))

  private def resolveMalware(spec: MalwareSpec): ValidationResult[Malware] =
    val infectivity = probability("infectivity", spec.infectivity)
    val stealth = probability("stealth", spec.stealth)
    val persistence = probability("persistence", spec.persistence)
    val footprint = probability("footprint", spec.footprint)

    val (probabilityErrors, _) =
      ValidationResult.partition(List(infectivity, stealth, persistence, footprint))

    if probabilityErrors.nonEmpty then Left(probabilityErrors)
    else
      for
        i <- infectivity
        s <- stealth
        p <- persistence
        f <- footprint
        built <- Malware(
          spec.name,
          spec.kind,
          MalwareTraits(i, s, spec.severity, p, f),
          spec.vectors
        ).left.map(List(_))
      yield built

  private def resolveStartingNode(topology: Topology, rawId: String): ValidationResult[Node] =
    val id = NodeId.normalize(rawId)
    topology.nodes.get(id).toRight(List(s"Starting node references unknown node id: $id"))

  /** Validates every declaration and assembles a `Scenario` through `Scenario.apply`, so the
    * domain's own rules (and the derived baseline workload) apply unchanged.
    *
    * Errors are accumulated across the network, the malware and the scenario-level fields. The
    * starting node is only resolved once both the network and its id are valid, so a broken network
    * doesn't also produce a misleading "unknown node id".
    *
    * @param defaultSeed
    *   evaluated only if no seed was declared
    */
  def build(defaultSeed: => Int): ValidationResult[Scenario] =
    val topologyResult: ValidationResult[Topology] =
      single(
        networks.toList,
        "The scenario must declare a network",
        "The network is declared more than once"
      ).flatMap(declared => declared)

    val malwareResult: ValidationResult[Malware] =
      single(
        malwares.toList,
        "The scenario must declare a malware",
        "The malware is declared more than once"
      ).flatMap(resolveMalware)

    val startingIdResult: ValidationResult[String] =
      single(
        startingNodeIds.toList,
        "The scenario must declare a starting node",
        "The starting node is declared more than once"
      )

    val startingNodeResult: ValidationResult[Node] =
      (topologyResult, startingIdResult) match
        case (Right(topology), Right(id)) => resolveStartingNode(topology, id)
        case _ => Left(Nil) // the cause is already reported by the two results above

    val seedResult = atMostOne(seeds.toList, "The seed is declared more than once")
    val iterationsResult =
      atMostOne(maxIterationValues.toList, "The max iterations are declared more than once")

    val countermeasureConfigResult: ValidationResult[CountermeasureConfig] =
      atMostOne(
        counterConfig.toList,
        "The countermeasures are declared more than once"
      ).flatMap {
        case Some(result) => result
        case None         => Right(CountermeasureConfig.empty)
      }

    val errors = errorsOf(
      topologyResult,
      malwareResult,
      startingIdResult,
      startingNodeResult,
      seedResult,
      iterationsResult,
      countermeasureConfigResult
    )

    if errors.nonEmpty then Left(errors)
    else
      for
        topology <- topologyResult
        virus <- malwareResult
        startNode <- startingNodeResult
        declaredSeed <- seedResult
        declaredIterations <- iterationsResult
        config <- countermeasureConfigResult
        built <- Scenario(
          name = name,
          topology = topology,
          virus = virus,
          startingNode = startNode,
          tick = 0,
          seed = declaredSeed.getOrElse(defaultSeed),
          maxIterations = declaredIterations.getOrElse(SimulationConfig.Defaults.MAX_ITERATIONS),
          countermeasureConfig = config
        ).left.map(List(_))
      yield built
