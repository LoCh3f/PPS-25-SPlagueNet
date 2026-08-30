package it.unibo.splague.update

import it.unibo.splague.model.Probability
import it.unibo.splague.model.connection.Connection
import it.unibo.splague.model.malware.MalwareKind.Worm
import it.unibo.splague.model.malware.{
  Malware,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import it.unibo.splague.simulation.event.Infection
import it.unibo.splague.simulation.{Scenario, SimulationEngine}
import it.unibo.splague.update.Mvu.{ModelState, Msg, Screen, update}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class MvuIntegrationSuite extends AnyFunSuite with Matchers:

  private val packetLoss = Probability.apply(0.1).getOrElse(fail("packet loss should be valid"))

  private val channel = Connection.Channel(
    channelType = Connection.ChannelType.VPN,
    bandwidth = 250.0,
    latency = 15.0,
    jitter = 1.0,
    packetLoss = packetLoss
  )

  private def buildNode(
      id: String,
      tipo: NodeType,
      defense: Double,
      patch: Double,
      stato: NodeState
  ): Node =
    val nodeId = NodeId.of(id).getOrElse(fail(s"id non valido: $id"))
    Node(nodeId, tipo, patch, defense, stato, workload = 0.0)

  private val validTraits = (for
    infectivity <- Probability(1.0)
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

  test("Msg.Step, called trough Mvu.update, must propagate the virus to a node with null defences"):
    val src =
      buildNode("n1", NodeType.Workstation, defense = 0.0, patch = 0.0, stato = NodeState.Infected)
    val dst =
      buildNode("n2", NodeType.Workstation, defense = 0.0, patch = 0.0, stato = NodeState.Healthy)
    val edge = Connection.Edge(src, dst, channel, None)

    val topology = Topology(
      nodes = Map(src.nodeId.value -> src, dst.nodeId.value -> dst),
      edges = Set(edge)
    )

    val initialScenario = Scenario
      .apply(
        name = "Simulation Alpha",
        topology = topology,
        virus = dummyVirus,
        startingNode = src,
        tick = 0,
        seed = 42,
        maxIterations = 10
        // TODO ADD COUNTERMEASURES
        // TODO ADD AWARENESS
      )
      .getOrElse(fail("Error when creating scenario"))

    val initialModel = ModelState(
      screen =
        Screen.Simulation(SimulationEngine(_ => Infection.InfectionEvent).run(initialScenario))
    )

    val afterFirstStep = update(Msg.Step, initialModel)

    afterFirstStep.screen match
      case Screen.Simulation(remaining) =>
        val updatedState = remaining.head
        updatedState.topology.nodes(dst.nodeId.value).state shouldBe NodeState.Infected
      case other => fail(s"waiting Screen.Simulation, obtained $other")

  test("Msg.Step repeated must eventually infect the whole net") {
    val n1 = buildNode("n1", NodeType.Workstation, 0.0, 0.0, NodeState.Infected)
    val n2 = buildNode("n2", NodeType.Workstation, 0.0, 0.0, NodeState.Healthy)
    val n3 = buildNode("n3", NodeType.Workstation, 0.0, 0.0, NodeState.Healthy)

    val edge12 = Connection.Edge(n1, n2, channel, None)
    val edge23 = Connection.Edge(n2, n3, channel, None)

    val topology = Topology(
      nodes = Map(n1.nodeId.value -> n1, n2.nodeId.value -> n2, n3.nodeId.value -> n3),
      edges = Set(edge12, edge23)
    )

    val initialScenario = Scenario
      .apply(
        name = "Simulation Alpha",
        topology = topology,
        virus = dummyVirus,
        startingNode = n1,
        tick = 0,
        seed = 42,
        maxIterations = 10
        // TODO ADD COUNTERMEASURES
        // TODO ADD AWARENESS
      )
      .getOrElse(fail("Error when creating scenario"))

    val initialModel = ModelState(
      screen =
        Screen.Simulation(SimulationEngine(_ => Infection.InfectionEvent).run(initialScenario))
    )

    val finalModel = (1 to 5).foldLeft(initialModel)((m, _) => update(Msg.Step, m))

    finalModel.screen match
      case Screen.Simulation(remaining) =>
        remaining.head.topology.nodes.values.forall(_.state == NodeState.Infected) shouldBe true
      case other => fail(s"waiting Screen.Simulation, obtained $other")
  }
