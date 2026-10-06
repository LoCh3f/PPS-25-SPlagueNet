package it.unibo.splague.update.messages

import it.unibo.splague.AppState
import it.unibo.splague.model.Scenario
import it.unibo.splague.model.node.NodeState
import it.unibo.splague.update.Msg
import it.unibo.splague.update.simulation.event.SimulationEvents.{Event, EventSelector}
import it.unibo.splague.update.simulation.event.*
import it.unibo.splague.update.simulation.{SimulationEngine, SimulationState}
import it.unibo.splague.view.ValidationError
import it.unibo.splague.view.form.ScenarioForm

/** Handles the simulation run lifecycle: [[Msg.StartSimulation]], [[Msg.SimulationStep]],
  * [[Msg.ToggleSimulationPause]], [[Msg.ResetSimulation]].
  */
// $COVERAGE-OFF$
object SimulationUpdate:

  def update(msg: Msg, state: AppState): AppState = msg match

    case Msg.StartSimulation =>
      // Re-pressing Run while a simulation already exists would treat whatever's currently in
      // scenarioForm (a live mid-run/paused/finished snapshot) as a brand-new scenario to seed
      // and run, silently discarding the existing SimulationState and corrupting the pre-seed
      // model.currentScenario ResetSimulation depends on. Reset must happen first.
      if state.simulation.isDefined then
        state.copy(
          errors = Vector(
            ValidationError(
              "simulation",
              "A simulation is already in progress; reset it before starting a new one"
            )
          )
        )
      else
        state.scenarioForm match
          case None =>
            state.copy(
              errors = Vector(
                ValidationError("scenarioForm", "No scenario form is open")
              )
            )

          case Some(form) =>
            ScenarioForm.toDomain(form) match
              case Left(error) =>
                state.copy(errors = Vector(ValidationError("scenario", error)))

              case Right(scenario) =>
                val seeded = seedOutbreak(scenario)
                val states =
                  new SimulationEngine(simulationSelector).run(seeded)

                states match
                  case current #:: upcoming =>
                    state.copy(
                      simulation = Some(
                        SimulationState(
                          initial = seeded,
                          selector = simulationSelector,
                          states = upcoming,
                          current = current,
                          running = upcoming.nonEmpty
                        )
                      ),
                      // model.currentScenario keeps the pre-seed scenario (patient zero still
                      // Healthy), not `current`/`seeded` — it's the identity of the scenario this
                      // session is working on, not a snapshot of the run. This is what
                      // ResetSimulation restores: without it, Reset would bring back patient zero
                      // already infected instead of a clean, all-Healthy scenario.
                      model = state.model.copy(currentScenario = Some(scenario)),
                      errors = Vector.empty
                    )

                  case _ =>
                    state.copy(
                      errors = Vector(
                        ValidationError("scenario", "Unable to start the simulation")
                      )
                    )

    case Msg.SimulationStep =>
      state.simulation match

        // Paused simulations still receive this message every tick (Runtime's Timer never
        // stops), it just does nothing with it until resumed — same as an already-finished one.
        case Some(simulation) if simulation.running && !simulation.paused =>
          val nextSimulation =
            simulation.next

          // model.currentScenario deliberately stays untouched here: it identifies the scenario
          // this session is working on (set on open/select/save/cancel/start/reset/import), while
          // scenarioForm alone drives the live, tick-by-tick view.
          state.copy(
            simulation = Some(nextSimulation),
            scenarioForm = Some(ScenarioForm.fromScenario(nextSimulation.current)),
            errors = Vector.empty
          )

        case _ => state

    case Msg.ToggleSimulationPause =>
      state.simulation match
        case Some(simulation) if simulation.running =>
          state.copy(simulation = Some(simulation.togglePause), errors = Vector.empty)

        case Some(_) =>
          state.copy(
            errors = Vector(ValidationError("simulation", "Simulation has already finished"))
          )

        case None =>
          state.copy(
            errors = Vector(ValidationError("simulation", "No simulation to pause or resume"))
          )

    case Msg.ResetSimulation =>
      state.simulation match
        // A paused simulation can also be reset, not just a finished one: otherwise pausing
        // would be a dead end, forcing it to be resumed to completion before it could be reset.
        case Some(simulation) if !simulation.running || simulation.paused =>
          // Restores model.currentScenario, not simulation.initial: the latter is deliberately
          // the *seeded* scenario (patient zero already Infected), since ScenarioReport.from
          // replays it through the engine to reconstruct the run's timeline and would show no
          // outbreak at all if it weren't seeded. model.currentScenario, by contrast, is kept at
          // the pre-seed scenario by StartSimulation, so Reset brings back a genuinely clean,
          // all-Healthy scenario instead of one with patient zero already infected.
          state.model.currentScenario match
            case Some(scenario) =>
              state.copy(
                simulation = None,
                scenarioForm = Some(ScenarioForm.fromScenario(scenario)),
                errors = Vector.empty
              )

            case None =>
              state.copy(
                errors = Vector(ValidationError("scenario", "No scenario to reset to"))
              )

        case Some(_) =>
          state.copy(
            errors = Vector(ValidationError("simulation", "Simulation is still running"))
          )

        case None =>
          state.copy(
            errors = Vector(ValidationError("simulation", "No simulation to reset"))
          )

  /** Marks the scenario's starting node as the outbreak's patient zero. A scenario's `startingNode`
    * is only a topology reference; nothing else ever infects it, so without this every node stays
    * `Healthy` forever and the simulation runs to completion doing nothing observable.
    */
  private def seedOutbreak(scenario: Scenario): Scenario =
    val startingId = scenario.startingNode.nodeId.value

    scenario.topology.nodes.get(startingId) match
      case Some(node) if node.state != NodeState.Infected =>
        val infected = node.copy(state = NodeState.Infected)

        scenario.copy(
          topology = scenario.topology.copy(
            nodes = scenario.topology.nodes.updated(startingId, infected)
          ),
          startingNode = infected
        )

      case _ => scenario

  /** Cycles one composite event per tick (tick % 4): awareness/activation, containment defenses,
    * infection spread + prevention boosts, then cure/destroy. Groups the 7 event categories into 4
    * slots since [[TickBasedCyclicSelector]] cycles a fixed set of 4 events, one per tick.
    */
  private def simulationSelector: EventSelector =
    TickBasedCyclicSelector(
      combine(Detection, CountermeasureActivation.ActivationEvent),
      combine(Defense.IsolationEvent, Defense.FirewallEvent),
      combine(Infection.InfectionEvent, Prevention.PatchBoostEvent, Prevention.DefenseBoostEvent),
      combine(
        Cure.CureEvent,
        Cure.LowerWorkloadEvent,
        Destroy.IncreaseWorkloadEvent,
        Destroy.DestroyEvent
      )
    )

  private def combine(events: Event*): Event =
    (scenario: Scenario) => events.foldLeft(scenario)((acc, event) => event(acc))
// $COVERAGE-ON$
