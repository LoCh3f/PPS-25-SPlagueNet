package it.unibo.splague.utils

import it.unibo.splague.model.node.NodeState
import it.unibo.splague.model.node.NodeState.Infected
import it.unibo.splague.update.simulation.SimulationEngine
import it.unibo.splague.update.simulation.event.{
  CountermeasureActivation,
  Cure,
  Defense,
  Destroy,
  Detection,
  Infection,
  Prevention,
  TickBasedSelector
}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ExampleScenarioSuite extends AnyFunSuite:

  test("complex scenario should complete a full tick-based simulation until maxIterations"):
    val initialScenario =
      ExampleScenario.complexScenario().getOrElse(fail("complex scenario should be valid"))

    val selector = new TickBasedSelector(
      Vector(
        Infection.InfectionEvent
      )
    )

    val states = new SimulationEngine(selector).run(initialScenario).toList
    println(initialScenario.topology.infectedNodes().size)
    println(states.size)
    println(states(4).topology.infectedNodes().size)
    println(states.count(s => s.topology.nodes.exists((_, n) => n.state == NodeState.Infected)))

    states.nonEmpty shouldBe true
    states.size shouldBe initialScenario.maxIterations + 1
    states.head.tick shouldBe 0
    states.last.tick shouldBe initialScenario.maxIterations
    states.forall(_.tick <= initialScenario.maxIterations) shouldBe true
