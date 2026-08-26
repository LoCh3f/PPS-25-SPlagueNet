package it.unibo.splague.simulation

import it.unibo.splague.model.Probability
import it.unibo.splague.model.malware.{
  Malware,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.malware.MalwareKind.Worm
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import org.junit.runner.RunWith
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class ScenarioSuite extends AnyFunSuite with Matchers with EitherValues:
  private val id1 = NodeId.of("node-01").getOrElse(fail())
  private val id2 = NodeId.of("node-02").getOrElse(fail())

  private val nodeValid = Node(id1, NodeType.Router, 0.1, 0.2, NodeState.Healthy, 0.0)
  private val nodeInvalid = Node(id2, NodeType.Server, 0.0, 0.1, NodeState.Healthy, 0.0)

  private val topology = Topology(
    nodes = Map("node-01" -> nodeValid),
    edges = Set.empty
  )

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

  private val dummyVirus = Malware(
    "dummy",
    Worm,
    validTraits,
    vectors = Set(PropagationVector.NetworkExploit)
  ).toOption.get

  test("Scenario creation should succeed with valid parameters"):
    val result = Scenario(
      name = "Simulation Alpha",
      topology = topology,
      virus = dummyVirus,
      startingNode = nodeValid,
      tick = 0,
      seed = 42,
      maxIterations = 100
    )

    result shouldBe a[Right[?, ?]]

  test("Scenario creation should fail if name is empty"):

    val result = Scenario(
      name = "   ",
      topology = topology,
      virus = dummyVirus,
      startingNode = nodeValid,
      tick = 0,
      seed = 42,
      maxIterations = 100
    )

    result shouldBe a[Left[?, ?]]
    result.left.get should include("name")

  test("Scenario creation should fail if maxIterations is not positive"):
    val result = Scenario(
      name = "Simulation Alpha",
      topology = topology,
      virus = dummyVirus,
      startingNode = nodeValid,
      tick = 0,
      seed = 42,
      maxIterations = 0
    )

    result shouldBe a[Left[?, ?]]

  test("Scenario creation should fail if startingNode does not exist in topology"):
    val result = Scenario(
      name = "Simulation Alpha",
      topology = topology,
      virus = dummyVirus,
      startingNode = nodeInvalid, // Non presente nella topologia
      tick = 0,
      seed = 42,
      maxIterations = 100
    )

    result shouldBe a[Left[?, ?]]
