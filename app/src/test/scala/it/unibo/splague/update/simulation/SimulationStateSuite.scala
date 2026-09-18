package it.unibo.splague.update.simulation

import it.unibo.splague.model.{Probability, Scenario}
import it.unibo.splague.model.malware.{
  Malware,
  MalwareKind,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import it.unibo.splague.update.simulation.event.SimulationEvents.{Event, EventSelector}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SimulationStateSuite extends AnyFunSuite:

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

  private def atTick(tick: Int): Scenario = baseline.copy(tick = tick)

  test("currentTick returns the tick of the current scenario"):
    val state = SimulationState(
      initial = baseline,
      selector = noOpSelector,
      states = LazyList.empty,
      current = atTick(2),
      running = true
    )

    state.currentTick shouldBe 2

  test("isFinished is true when there are no upcoming states left"):
    val state = SimulationState(
      initial = baseline,
      selector = noOpSelector,
      states = LazyList.empty,
      current = atTick(1),
      running = true
    )

    state.isFinished shouldBe true

  test("isFinished is true once the current tick reaches maxIterations, even with states left"):
    val state = SimulationState(
      initial = baseline,
      selector = noOpSelector,
      states = LazyList(atTick(4)),
      current = atTick(3),
      running = true
    )

    state.isFinished shouldBe true

  test(
    "isFinished is false while there are upcoming states and maxIterations has not been reached"
  ):
    val state = SimulationState(
      initial = baseline,
      selector = noOpSelector,
      states = LazyList(atTick(1)),
      current = atTick(0),
      running = true
    )

    state.isFinished shouldBe false

  test("next advances to the head of states, keeping the rest as the remaining states"):
    val state = SimulationState(
      initial = baseline,
      selector = noOpSelector,
      states = LazyList(atTick(1), atTick(2)),
      current = atTick(0),
      running = true
    )

    val advanced = state.next

    advanced.current shouldBe atTick(1)
    advanced.states.toList shouldBe List(atTick(2))

  test("next stays running while more states remain after advancing"):
    val state = SimulationState(
      initial = baseline,
      selector = noOpSelector,
      states = LazyList(atTick(1), atTick(2)),
      current = atTick(0),
      running = true
    )

    state.next.running shouldBe true

  test("next stops running once the states it advances into are the last one"):
    val state = SimulationState(
      initial = baseline,
      selector = noOpSelector,
      states = LazyList(atTick(1)),
      current = atTick(0),
      running = true
    )

    val advanced = state.next

    advanced.current shouldBe atTick(1)
    advanced.running shouldBe false

  test("next on an empty states LazyList leaves current unchanged and stops running"):
    val state = SimulationState(
      initial = baseline,
      selector = noOpSelector,
      states = LazyList.empty,
      current = atTick(3),
      running = true
    )

    val advanced = state.next

    advanced.current shouldBe atTick(3)
    advanced.states.isEmpty shouldBe true
    advanced.running shouldBe false

  test("SimulationState carries the initial scenario and selector it was created with"):
    val state = SimulationState(
      initial = baseline,
      selector = noOpSelector,
      states = LazyList.empty,
      current = baseline,
      running = false
    )

    state.initial shouldBe baseline
    state.selector shouldBe noOpSelector

  test("next preserves initial and selector across an advance"):
    val state = SimulationState(
      initial = baseline,
      selector = noOpSelector,
      states = LazyList(atTick(1), atTick(2)),
      current = atTick(0),
      running = true
    )

    val advanced = state.next

    advanced.initial shouldBe baseline
    advanced.selector shouldBe noOpSelector
