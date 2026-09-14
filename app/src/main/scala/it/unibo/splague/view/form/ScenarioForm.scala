package it.unibo.splague.view.form

import it.unibo.splague.model.*
import it.unibo.splague.model.Awareness.*
import it.unibo.splague.model.node.{NodeId, Topology}

final case class ScenarioForm(
    name: String,
    topology: TopologyForm,
    virus: MalwareForm,
    startingNodeId: String,
    tick: String,
    seed: String,
    maxIterations: String,
    awareness: Double,
    countermeasureConfig: CountermeasureForm
)

object ScenarioForm:

  def fromScenario(scenario: Scenario): ScenarioForm =
    ScenarioForm(
      name = scenario.name,
      topology = TopologyForm.fromTopology(scenario.topology),
      virus = MalwareForm.fromMalware(scenario.virus),
      startingNodeId = scenario.startingNode.nodeId.value,
      tick = scenario.tick.toString,
      seed = scenario.seed.toString,
      maxIterations = scenario.maxIterations.toString,
      awareness = scenario.awareness.value,
      countermeasureConfig = CountermeasureForm.fromDomain(scenario.countermeasureConfig)
    )

  def toDomain(form: ScenarioForm): Either[String, Scenario] =
    for
      topology <- TopologyForm.toDomain(form.topology)
      malware <- MalwareForm.toDomain(form.virus)

      startingId <- NodeId.of(form.startingNodeId)
      startingNode <- topology.nodes
        .get(startingId.value)
        .toRight(s"Starting node '${form.startingNodeId}' not found in topology")

      tick <- parseInt(form.tick, "tick")
      seed <- parseInt(form.seed, "seed")
      maxIterations <- parseInt(form.maxIterations, "maxIterations")
      awareness <- Awareness(form.awareness)
      counter <- CountermeasureForm.toDomain(form.countermeasureConfig)

      scenario <- Scenario(
        name = form.name,
        topology = topology,
        virus = malware,
        startingNode = startingNode,
        tick = tick,
        seed = seed,
        maxIterations = maxIterations,
        awareness = awareness,
        countermeasureConfig = counter
      )
    yield scenario

  private def parseInt(s: String, field: String): Either[String, Int] =
    s.trim.toIntOption.toRight(s"$field must be an integer, got '$s'")
