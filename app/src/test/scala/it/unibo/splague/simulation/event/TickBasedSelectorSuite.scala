package it.unibo.splague.simulation.event

import it.unibo.splague.model.Probability
import it.unibo.splague.model.malware.{
  Malware,
  MalwareKind,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import it.unibo.splague.simulation.Scenario
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TickBasedSelectorSuite extends AnyFunSuite:

  private val traits = (for
    infectivity <- Probability(0.5)
    stealth <- Probability(0.5)
    persistence <- Probability(0.5)
    footprint <- Probability(0.5)
  yield MalwareTraits(
    infectivity,
    stealth,
    PayloadSeverityLevel.Low,
    persistence,
    footprint
  )).toOption.get

  private val malware =
    Malware("m", MalwareKind.Worm, traits, Set(PropagationVector.NetworkExploit)).toOption.get

  private val node =
    Node(NodeId.of("n").toOption.get, NodeType.Router, 0.0, 0.0, NodeState.Healthy, 0.0)
  private val topo = Topology(nodes = Map(node.nodeId.value -> node), edges = Set.empty)

  private def scenarioWithTick(t: Int) =
    Scenario("s", topo, malware, node, tick = t, seed = 0, maxIterations = 10).toOption.get

  test("empty selector should return identity event"):
    val selector = new TickBasedSelector(Vector.empty)
    val sc = scenarioWithTick(0)
    val ev = selector.nextEvent(sc)
    // applying the event must return the same scenario
    ev(sc) shouldBe sc

  test("selector should cycle through provided events using tick modulo size"):
    val e1 = new SimulationEvents.Event:
      override def apply(s: Scenario): Scenario = s.copy(name = "e1")
    val e2 = new SimulationEvents.Event:
      override def apply(s: Scenario): Scenario = s.copy(name = "e2")
    val e3 = new SimulationEvents.Event:
      override def apply(s: Scenario): Scenario = s.copy(name = "e3")

    val selector = new TickBasedSelector(Vector(e1, e2, e3))

    selector.nextEvent(scenarioWithTick(0)).apply(scenarioWithTick(0)).name shouldBe "e1"
    selector.nextEvent(scenarioWithTick(1)).apply(scenarioWithTick(1)).name shouldBe "e2"
    selector.nextEvent(scenarioWithTick(2)).apply(scenarioWithTick(2)).name shouldBe "e3"
    // tick 3 wraps back to e1
    selector.nextEvent(scenarioWithTick(3)).apply(scenarioWithTick(3)).name shouldBe "e1"

  test("TickBasedCyclicSelector.apply should build a 4-element selector"):
    val a = new SimulationEvents.Event:
      override def apply(s: Scenario): Scenario = s.copy(name = "a")
    val b = new SimulationEvents.Event:
      override def apply(s: Scenario): Scenario = s.copy(name = "b")
    val c = new SimulationEvents.Event:
      override def apply(s: Scenario): Scenario = s.copy(name = "c")
    val d = new SimulationEvents.Event:
      override def apply(s: Scenario): Scenario = s.copy(name = "d")

    val sel = TickBasedCyclicSelector(a, b, c, d)
    sel.nextEvent(scenarioWithTick(0)).apply(scenarioWithTick(0)).name shouldBe "a"
    sel.nextEvent(scenarioWithTick(1)).apply(scenarioWithTick(1)).name shouldBe "b"
    sel.nextEvent(scenarioWithTick(2)).apply(scenarioWithTick(2)).name shouldBe "c"
    sel.nextEvent(scenarioWithTick(3)).apply(scenarioWithTick(3)).name shouldBe "d"
    sel.nextEvent(scenarioWithTick(4)).apply(scenarioWithTick(4)).name shouldBe "a"
