package it.unibo.splague.update.messages

import it.unibo.splague.AppState
import it.unibo.splague.model.malware.*
import it.unibo.splague.model.node.*
import it.unibo.splague.model.{ModelState, Probability, Scenario}
import it.unibo.splague.update.Msg
import it.unibo.splague.update.messages.SimulationUpdate
import it.unibo.splague.update.simulation.SimulationState
import it.unibo.splague.update.simulation.event.SimulationEvents.{Event, EventSelector}
import it.unibo.splague.view.form.ScenarioForm
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class SimulationUpdateSuite extends AnyFunSuite with Matchers:

  private val validTraits = (for
    infectivity <- Probability(0.6)
    stealth <- Probability(0.4)
    persistence <- Probability(0.5)
    footprint <- Probability(0.3)
  yield MalwareTraits(
    infectivity,
    stealth,
    payloadSeverity = PayloadSeverityLevel.Low,
    persistence,
    footprint
  )).toOption.get

  private val malware = Malware(
    name = "TestMalware",
    kind = MalwareKind.Worm,
    traits = validTraits,
    vectors = Set(PropagationVector.NetworkExploit)
  ).toOption.get

  private val node = Node(
    NodeId.of("node-A").toOption.get,
    NodeType.Router,
    patchLevel = 0.2,
    defenseLevel = 0.5,
    state = NodeState.Healthy,
    workload = 0.3,
    Set()
  )

  private val topology = Topology(
    nodes = Map(node.nodeId.value -> node),
    edges = Set.empty
  )

  private val baseline = Scenario(
    name = "Baseline",
    topology = topology,
    virus = malware,
    startingNode = node,
    tick = 0,
    seed = 7,
    maxIterations = 3
  ).toOption.get

  private val noOpEvent: Event = (s: Scenario) => s
  private val noOpSelector: EventSelector = (_: Scenario) => noOpEvent

  private def stateWithSimulation(running: Boolean): AppState =
    AppState
      .init(ModelState())
      .copy(
        simulation = Some(
          SimulationState(
            initial = baseline,
            selector = noOpSelector,
            states = LazyList(baseline),
            current = baseline,
            running = running
          )
        )
      )

  test("SimulationStep advances the simulation and scenarioForm without touching currentScenario"):
    val advanced = baseline.copy(tick = 1)

    val stepping = AppState
      .init(ModelState(currentScenario = Some(baseline)))
      .copy(
        scenarioForm = Some(ScenarioForm.fromScenario(baseline)),
        simulation = Some(
          SimulationState(
            initial = baseline,
            selector = noOpSelector,
            states = LazyList(advanced),
            current = baseline,
            running = true
          )
        )
      )

    val result = SimulationUpdate.update(Msg.SimulationStep, stepping)

    result.simulation.map(_.current) shouldBe Some(advanced)
    result.scenarioForm.map(_.tick) shouldBe Some(advanced.tick.toString)
    // model.currentScenario identifies the scenario this session is working on (set on
    // open/select/save/cancel/start/reset/import); it must not follow every simulated tick.
    result.model.currentScenario shouldBe Some(baseline)

  test("SimulationStep does nothing while the simulation is paused"):
    val advanced = baseline.copy(tick = 1)

    val paused = AppState
      .init(ModelState())
      .copy(
        scenarioForm = Some(ScenarioForm.fromScenario(baseline)),
        simulation = Some(
          SimulationState(
            initial = baseline,
            selector = noOpSelector,
            states = LazyList(advanced),
            current = baseline,
            running = true,
            paused = true
          )
        )
      )

    val result = SimulationUpdate.update(Msg.SimulationStep, paused)

    // Runtime's Timer still fires every tick regardless of pause state; the handler must no-op.
    result shouldBe paused

  test("SimulationStep does nothing when there is no simulation"):
    val noSimulation = AppState.init(ModelState())

    val result = SimulationUpdate.update(Msg.SimulationStep, noSimulation)

    result shouldBe noSimulation

  test("ToggleSimulationPause flips paused back and forth on a running simulation"):
    val running = stateWithSimulation(running = true)

    val pausedResult = SimulationUpdate.update(Msg.ToggleSimulationPause, running)
    pausedResult.simulation.map(_.paused) shouldBe Some(true)
    pausedResult.errors shouldBe Vector.empty

    val resumedResult = SimulationUpdate.update(Msg.ToggleSimulationPause, pausedResult)
    resumedResult.simulation.map(_.paused) shouldBe Some(false)

  test("ToggleSimulationPause is refused when the simulation has already finished"):
    val finished = stateWithSimulation(running = false)

    val result = SimulationUpdate.update(Msg.ToggleSimulationPause, finished)

    result.simulation shouldBe finished.simulation
    result.errors should not be Vector.empty

  test("ToggleSimulationPause is refused when there is no simulation"):
    val noSimulation = AppState.init(ModelState())

    val result = SimulationUpdate.update(Msg.ToggleSimulationPause, noSimulation)

    result.errors should not be Vector.empty

  test("StartSimulation seeds simulation.initial but keeps model.currentScenario pre-seed"):
    val starting = AppState
      .init(ModelState())
      .copy(scenarioForm = Some(ScenarioForm.fromScenario(baseline)))

    val result = SimulationUpdate.update(Msg.StartSimulation, starting)

    result.simulation.map(_.initial.topology.nodes(node.nodeId.value).state) shouldBe
      Some(NodeState.Infected)
    // model.currentScenario is the scenario's identity, not a run snapshot: it must stay at the
    // pre-seed, all-Healthy scenario so ResetSimulation can restore a genuinely clean scenario.
    result.model.currentScenario.map(_.topology.nodes(node.nodeId.value).state) shouldBe
      Some(NodeState.Healthy)
    result.errors shouldBe Vector.empty

  test("StartSimulation is refused while a simulation already exists"):
    val alreadyRunning = stateWithSimulation(running = true)
      .copy(scenarioForm = Some(ScenarioForm.fromScenario(baseline)))

    val result = SimulationUpdate.update(Msg.StartSimulation, alreadyRunning)

    // Re-pressing Run must not discard the existing SimulationState or reseed anything.
    result.simulation shouldBe alreadyRunning.simulation
    result.errors should not be Vector.empty

  test("StartSimulation reports an error when no scenario form is open"):
    val state = AppState.init(ModelState())

    val result = SimulationUpdate.update(Msg.StartSimulation, state)

    result.errors should not be Vector.empty

  test(
    "ResetSimulation restores model.currentScenario, which stays Healthy though simulation.initial was seeded"
  ):
    val seededNode = node.copy(state = NodeState.Infected)
    val seeded = baseline.copy(
      topology = topology.copy(nodes = topology.nodes.updated(node.nodeId.value, seededNode))
    )

    val finished = AppState
      .init(ModelState(currentScenario = Some(baseline))) // pre-seed: patient zero still Healthy
      .copy(
        scenarioForm = Some(ScenarioForm.fromScenario(seeded)),
        simulation = Some(
          SimulationState(
            initial = seeded, // deliberately different: ScenarioReport needs this one seeded
            selector = noOpSelector,
            states = LazyList.empty,
            current = seeded,
            running = false
          )
        )
      )

    val result = SimulationUpdate.update(Msg.ResetSimulation, finished)

    result.simulation shouldBe None
    result.model.currentScenario shouldBe Some(baseline)
    result.scenarioForm.get.topology.nodes.map(_.state) shouldBe Vector(NodeState.Healthy)
    result.errors shouldBe Vector.empty

  test(
    "ResetSimulation restores model.currentScenario even when it diverged from simulation.initial"
  ):
    // Simulates saving mid-run: model.currentScenario was overwritten by SaveScenario with
    // whatever the form held at that point (here: a different, already-infected scenario), so
    // Reset should now restore *that*, not simulation.initial.
    val advanced = baseline.copy(tick = 1)
    val savedMidRun = baseline.copy(name = "Baseline (saved mid-run)", tick = 1)

    val finished = AppState
      .init(ModelState(currentScenario = Some(savedMidRun)))
      .copy(
        scenarioForm = Some(ScenarioForm.fromScenario(advanced)),
        simulation = Some(
          SimulationState(
            initial = baseline,
            selector = noOpSelector,
            states = LazyList.empty,
            current = advanced,
            running = false
          )
        )
      )

    val result = SimulationUpdate.update(Msg.ResetSimulation, finished)

    result.simulation shouldBe None
    result.model.currentScenario shouldBe Some(savedMidRun)
    result.scenarioForm.map(_.name) shouldBe Some(savedMidRun.name)
    result.errors shouldBe Vector.empty

  test("ResetSimulation is refused when there is a finished simulation but no current scenario"):
    val finishedNoScenario = AppState
      .init(ModelState())
      .copy(
        simulation = Some(
          SimulationState(
            initial = baseline,
            selector = noOpSelector,
            states = LazyList.empty,
            current = baseline,
            running = false
          )
        )
      )

    val result = SimulationUpdate.update(Msg.ResetSimulation, finishedNoScenario)

    result.simulation shouldBe finishedNoScenario.simulation
    result.errors should not be Vector.empty

  test("ResetSimulation succeeds on a paused simulation, not just a finished one"):
    val paused = AppState
      .init(ModelState(currentScenario = Some(baseline)))
      .copy(
        simulation = Some(
          SimulationState(
            initial = baseline,
            selector = noOpSelector,
            states = LazyList(baseline), // ticks remain: running stays true while paused
            current = baseline,
            running = true,
            paused = true
          )
        )
      )

    val result = SimulationUpdate.update(Msg.ResetSimulation, paused)

    result.simulation shouldBe None
    result.model.currentScenario shouldBe Some(baseline)
    result.errors shouldBe Vector.empty

  test("ResetSimulation is refused while the simulation is still running and not paused"):
    val running = stateWithSimulation(running = true)

    val result = SimulationUpdate.update(Msg.ResetSimulation, running)

    result.simulation shouldBe running.simulation
    result.errors should not be Vector.empty

  test("ResetSimulation is refused when there is no simulation to reset"):
    val noSimulation = AppState.init(ModelState())

    val result = SimulationUpdate.update(Msg.ResetSimulation, noSimulation)

    result.errors should not be Vector.empty
