package it.unibo.splague.simulation.event

import it.unibo.splague.model.{Awareness, Probability}
import it.unibo.splague.model.countermeasures.{CountermeasureConfig, Countermeasures}
import it.unibo.splague.model.malware.{
  Malware,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.malware.MalwareKind.Worm
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import it.unibo.splague.simulation.Scenario
import org.junit.runner.RunWith
import org.scalatest.Assertions.fail
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.should
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CountermeasureActivationSuite extends AnyFunSuite:
  private val nodeId = NodeId.of("node-1").getOrElse(fail())
  private val node = Node(nodeId, NodeType.Workstation, 0.0, 0.0, NodeState.Healthy, 0.0, Set())
  private val topology = Topology(Map("node-1" -> node), Set.empty)

  private val validTraits = (for
    infectivity <- Probability(0.5)
    stealth <- Probability(0.5)
    persistence <- Probability(0.5)
    footprint <- Probability(0.5)
  yield MalwareTraits(
    infectivity,
    stealth,
    payloadSeverity = PayloadSeverityLevel.Low,
    persistence,
    footprint
  )).toOption.get

  private val dummyVirus = Malware(
    "dummy",
    Worm,
    traits = validTraits,
    Set(PropagationVector.NetworkExploit)
  ).getOrElse(fail())

  test("Activation activates countermeasure when awareness meets or exceeds the threshold"):
    val config = CountermeasureConfig(
      activeCountermeasures = Set.empty,
      countermeasureLevels = Map(0.5 -> Countermeasures.Patch)
    ).getOrElse(fail())

    val scenario = Scenario(
      name = "Activation Test",
      topology = topology,
      virus = dummyVirus,
      startingNode = node,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = config,
      awareness = Awareness(0.6).getOrElse(fail())
    )

    val updatedScenario = CountermeasureActivation.ActivationEvent(scenario)

    updatedScenario.countermeasureConfig.activeCountermeasures should contain(Countermeasures.Patch)

  test("Activation leaves countermeasure inactive when awareness is below the threshold"):
    val config = CountermeasureConfig(
      activeCountermeasures = Set.empty,
      countermeasureLevels = Map(0.8 -> Countermeasures.Patch)
    ).getOrElse(fail())

    val scenario = Scenario(
      name = "No Activation Test",
      topology = topology,
      virus = dummyVirus,
      startingNode = node,
      tick = 0,
      seed = 42,
      maxIterations = 10,
      countermeasureConfig = config,
      awareness = Awareness(0.4).getOrElse(fail())
    )

    val updatedScenario = CountermeasureActivation.ActivationEvent(scenario)

    updatedScenario.countermeasureConfig.activeCountermeasures should Matchers.not contain Countermeasures.Patch
