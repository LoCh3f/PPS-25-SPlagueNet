package it.unibo.splague.update.simulation.report

import it.unibo.splague.model.Scenario
import it.unibo.splague.model.countermeasures.Countermeasures
import it.unibo.splague.model.report.TickSummary
import it.unibo.splague.update.simulation.SimulationEngine
import it.unibo.splague.update.simulation.event.SimulationEvents.EventSelector

/** A tick-by-tick summary of a full simulation run, obtained by deterministically replaying
  * `initial` through `SimulationEngine`. Countermeasure activation ticks are observed directly from
  * `Scenario.countermeasureConfig.activeCountermeasures` on each replayed tick so this stays
  * correct even if activation rules change. This assumes activation is monotonic (a countermeasure,
  * once active, never deactivates).
  */
final case class ScenarioReport(
    scenarioName: String,
    seed: Int,
    malwareName: String,
    timeline: Vector[TickSummary],
    activationTicks: Map[Countermeasures, Int]
):
  def finalTick: TickSummary = timeline.last

object ScenarioReport:
  def from(initial: Scenario, selector: EventSelector): ScenarioReport =
    val (timeline, activationTicks) =
      SimulationEngine(selector)
        .run(initial)
        .foldLeft(
          (Vector.empty[TickSummary], Map.empty[Countermeasures, Int])
        ) { case ((accTimeline, accActivation), scenario) =>
          val newlyActive =
            scenario.countermeasureConfig.activeCountermeasures -- accActivation.keySet
          (
            accTimeline :+ TickSummary.from(scenario),
            accActivation ++ newlyActive.map(_ -> scenario.tick)
          )
        }
    ScenarioReport(initial.name, initial.seed, initial.virus.name, timeline, activationTicks)
