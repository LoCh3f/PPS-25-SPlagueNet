package it.unibo.splague.dsl

import it.unibo.splague.model.Scenario
import it.unibo.splague.model.malware.{MalwareKind, PayloadSeverityLevel, PropagationVector}

import scala.util.Random

/** Entry point for declaratively building a `Scenario`.
  *
  * Usage:
  * {{{
  * val result: ValidationResult[Scenario] = scenario("Outbreak"):
  *   network:
  *     ring("n", 4, Workstation, LAN)
  *   malware("Worm", Worm, infectivity = 0.8, stealth = 0.3, severity = Low,
  *           persistence = 0.5, footprint = 0.3, vectors = Set(NetworkExploit))
  *   startingNode("n0")
  * }}}
  *
  * The network, the malware and the starting node are mandatory; the seed and the max iterations
  * fall back to a default when not declared. Errors are accumulated across the whole declaration,
  * including those coming from the nested `network` block.
  *
  * @param seedSource
  *   evaluated at most once, only when no `seed(...)` is declared. It runs at build time, so the
  *   drawn seed is stored in the `Scenario` and the run stays reproducible. The default is never
  *   negative.
  */
def scenario(
    name: String,
    seedSource: => Int = Random.nextInt(Int.MaxValue)
)(block: ScenarioBuilder ?=> Unit): ValidationResult[Scenario] =
  given builder: ScenarioBuilder = new ScenarioBuilder(name)
  block(using builder)
  builder.build(seedSource)

/** Declares the scenario's network. Wraps a regular `topology { ... }` block, so every node and
  * edge validation (and the shapes: `star`, `ring`, `mesh`) works unchanged inside it.
  */
def network(block: TopologyBuilder ?=> Unit)(using scenarioBuilder: ScenarioBuilder): Unit =
  scenarioBuilder.setNetwork(topology(block))

/** Declares the scenario's malware. The probabilities are validated, not clamped: a value outside
  * `[0,1]` is reported as an error naming the field.
  */
def malware(
    name: String,
    kind: MalwareKind,
    infectivity: Double,
    stealth: Double,
    severity: PayloadSeverityLevel,
    persistence: Double,
    footprint: Double,
    vectors: Set[PropagationVector]
)(using scenarioBuilder: ScenarioBuilder): Unit =
  scenarioBuilder.setMalware(
    MalwareSpec(name, kind, infectivity, stealth, severity, persistence, footprint, vectors)
  )

/** Declares the outbreak's starting node by id; resolved against the built network, with the same
  * normalization applied to node ids.
  */
def startingNode(id: String)(using scenarioBuilder: ScenarioBuilder): Unit =
  scenarioBuilder.setStartingNode(id)

/** Fixes the seed, so the run is reproducible. Without it, the seed comes from `seedSource`. */
def seed(value: Int)(using scenarioBuilder: ScenarioBuilder): Unit =
  scenarioBuilder.setSeed(value)

/** Overrides the default `SimulationConfig.Defaults.MAX_ITERATIONS`. */
def maxIterations(value: Int)(using scenarioBuilder: ScenarioBuilder): Unit =
  scenarioBuilder.setMaxIterations(value)
