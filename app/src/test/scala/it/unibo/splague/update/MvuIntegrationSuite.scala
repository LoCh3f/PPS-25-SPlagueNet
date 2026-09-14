package it.unibo.splague.update

import it.unibo.splague.model.{Awareness, Probability, Scenario}
import it.unibo.splague.model.connection.Connection
import it.unibo.splague.model.connection.Protocol.{
  ApplicationProtocol,
  ApplicationProtocolType,
  TcpTransport,
  TransportProtocol
}
import it.unibo.splague.model.countermeasures.{CountermeasureConfig, Countermeasures}
import it.unibo.splague.model.malware.MalwareKind.Worm
import it.unibo.splague.model.malware.{
  Malware,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}
import it.unibo.splague.update.simulation.event.{
  CountermeasureActivation,
  Cure,
  Defense,
  Destroy,
  Detection,
  Infection,
  Prevention,
  SimulationEvents,
  TickBasedSelector
}
import it.unibo.splague.update.simulation.event.SimulationEvents.Event
import it.unibo.splague.update.simulation.{SimulationEngine}
import it.unibo.splague.update.Mvu.{ModelState, Msg, Screen, update}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import it.unibo.splague.utils.TestApplicationProtocol

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

  private val defaultEventVector = Vector(
    Detection,
    CountermeasureActivation.ActivationEvent,
    Prevention.DefenseBoostEvent,
    Prevention.PatchBoostEvent,
    Defense.IsolationEvent,
    Defense.FirewallEvent,
    Infection.InfectionEvent,
    Destroy.IncreaseWorkloadEvent,
    Cure.CureEvent,
    Cure.LowerWorkloadEvent,
    Destroy.DestroyEvent
  )
  private def buildNode(
      id: String,
      tipo: NodeType,
      defense: Double,
      patch: Double,
      stato: NodeState
  ): Node =
    val nodeId = NodeId.of(id).getOrElse(fail(s"id non valido: $id"))
    Node(nodeId, tipo, patch, defense, stato, workload = 0.3, Set())

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

  test(
    "an active Firewall prevents infection from crossing a filtered FTP edge, with no destruction event"
  ):
    val maxTraits = (for
      infectivity <- Probability(1.0); stealth <- Probability(0.0)
      persistence <- Probability(0.0); footprint <- Probability(0.0)
    yield MalwareTraits(
      infectivity,
      stealth,
      PayloadSeverityLevel.Low,
      persistence,
      footprint
    )).toOption.get
    val aggressiveMalware =
      Malware("test-max", Worm, maxTraits, Set(PropagationVector.NetworkExploit)).getOrElse(fail())

    val src =
      buildNode("n1", NodeType.Workstation, defense = 0.5, patch = 0.5, stato = NodeState.Infected)
    val dst =
      buildNode("n2", NodeType.Server, defense = 0.5, patch = 0.5, stato = NodeState.Healthy)
    val edge =
      Connection.Edge(src, dst, channel, Some(TestApplicationProtocol(ApplicationProtocolType.FTP)))

    val config = CountermeasureConfig(
      countermeasureLevels =
        Map(0.01 -> Countermeasures.Firewall) // low level: it gets triggered almost immediately
    ).getOrElse(fail())

    val scenario = Scenario
      .apply(
        "Firewall blocks propagation",
        Topology(Map(src.nodeId.value -> src, dst.nodeId.value -> dst), Set(edge)),
        aggressiveMalware,
        src,
        tick = 0,
        seed = 42,
        maxIterations = 100,
        countermeasureConfig = config
      )
      .getOrElse(fail())

    val eventVectorWithoutDestroy = Vector(
      Detection,
      CountermeasureActivation.ActivationEvent,
      Prevention.DefenseBoostEvent,
      Prevention.PatchBoostEvent,
      Defense.IsolationEvent,
      Defense.FirewallEvent,
      Infection.InfectionEvent,
      Destroy.IncreaseWorkloadEvent,
      Cure.CureEvent,
      Cure.LowerWorkloadEvent
    )

    val selector = new TickBasedSelector(eventVectorWithoutDestroy)
    val states = new SimulationEngine(selector).run(scenario).toList
    val finalScenario = states.last
    finalScenario.countermeasureConfig.activeCountermeasures should contain(
      Countermeasures.Firewall
    )
    finalScenario.topology.nodes(dst.nodeId.value).state shouldBe NodeState.Healthy

  test("Isolation followed by Patch eventually cures a quarantined node to Immune"):
    val src =
      buildNode("n1", NodeType.Workstation, defense = 0.0, patch = 0.0, stato = NodeState.Infected)
    val dst =
      buildNode("n2", NodeType.Server, defense = 0.0, patch = 0.0, stato = NodeState.Infected)
    val topology = Topology(Map(src.nodeId.value -> src, dst.nodeId.value -> dst), Set.empty)

    val config = CountermeasureConfig(
      countermeasureLevels = Map(0.01 -> Countermeasures.Isolation, 0.02 -> Countermeasures.Patch),
      isolationCriteria = IsolationCriteria.byType(Set(NodeType.Server))
    ).getOrElse(fail())

    val scenario = Scenario
      .apply(
        "Isolation then cure",
        topology,
        dummyVirus,
        src,
        tick = 0,
        seed = 42,
        maxIterations = 50, // many iterations
        countermeasureConfig = config
      )
      .getOrElse(fail())

    val selector = new TickBasedSelector(defaultEventVector)
    val states = new SimulationEngine(selector).run(scenario).toList
    val finalScenario = states.last

    finalScenario.topology.nodes(dst.nodeId.value).state shouldBe NodeState.Immune

  test(
    "with no active countermeasures and no destruction, an undefended reachable network gets fully infected"
  ):
    val maxTraits = (for
      infectivity <- Probability(1.0); stealth <- Probability(0.0)
      persistence <- Probability(0.0); footprint <- Probability(0.0)
    yield MalwareTraits(
      infectivity,
      stealth,
      PayloadSeverityLevel.Low,
      persistence,
      footprint
    )).toOption.get
    val aggressiveMalware =
      Malware("test-max", Worm, maxTraits, Set(PropagationVector.NetworkExploit)).getOrElse(fail())

    val srcId = NodeId.of("n1").getOrElse(fail("Failed to create node"))
    val src = Node(
      srcId,
      NodeType.Workstation,
      0.0,
      0.0,
      NodeState.Infected,
      0.3,
      Set(PropagationVector.NetworkExploit)
    )

    val dstId = NodeId.of("n2").getOrElse(fail("Failed to create node"))
    val dst = Node(
      dstId,
      NodeType.Workstation,
      0.0,
      0.0,
      NodeState.Healthy,
      0.3,
      Set(PropagationVector.NetworkExploit)
    )

    val edge = Connection.Edge(
      src,
      dst,
      channel,
      Some(TestApplicationProtocol(ApplicationProtocolType.HTTPS))
    )

    val config = CountermeasureConfig(countermeasureLevels = Map.empty)
      .getOrElse(fail()) // no counter. level defined

    val scenario = Scenario
      .apply(
        "No countermeasures",
        Topology(Map(src.nodeId.value -> src, dst.nodeId.value -> dst), Set(edge)),
        aggressiveMalware,
        src,
        tick = 0,
        seed = 42,
        maxIterations = 100,
        countermeasureConfig = config
      )
      .getOrElse(fail())

    val eventVectorWithoutDestroy = Vector(
      Detection,
      CountermeasureActivation.ActivationEvent,
      Prevention.DefenseBoostEvent,
      Prevention.PatchBoostEvent,
      Defense.IsolationEvent,
      Defense.FirewallEvent,
      Infection.InfectionEvent,
      Destroy.IncreaseWorkloadEvent,
      Cure.CureEvent,
      Cure.LowerWorkloadEvent
    )

    val selector = new TickBasedSelector(eventVectorWithoutDestroy)
    val states = new SimulationEngine(selector).run(scenario).toList
    val finalScenario = states.last

    finalScenario.countermeasureConfig.activeCountermeasures shouldBe empty
    finalScenario.topology.nodes(dst.nodeId.value).state shouldBe NodeState.Infected
