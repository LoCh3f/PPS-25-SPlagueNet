package it.unibo.splague.model.report

import it.unibo.splague.model.Scenario

final case class TickSummary(
    tick: Int,
    healthy: Int,
    infected: Int,
    quarantined: Int,
    immune: Int,
    destroyed: Int,
    awareness: Double
)

object TickSummary:
  def from(scenario: Scenario): TickSummary =
    TickSummary(
      tick = scenario.tick,
      healthy = scenario.topology.healthyNodes().size,
      infected = scenario.topology.infectedNodes().size,
      quarantined = scenario.topology.quarantinedNodes().size,
      immune = scenario.topology.immuneNodes().size,
      destroyed = scenario.topology.destroyedNodes().size,
      awareness = scenario.awareness.value
    )
