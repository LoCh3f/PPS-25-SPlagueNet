package it.unibo.splague.update.simulation.event.rules

import it.unibo.splague.model.Probability
import it.unibo.splague.model.connection.Connection
import it.unibo.splague.model.malware.*
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class ContagionRulesSuite extends AnyFunSuite with Matchers:

  private def validMalware(infectivity: Probability): Malware =
    val traits = MalwareTraits(
      infectivity = infectivity,
      stealth = Probability(0.4).toOption.get,
      payloadSeverity = PayloadSeverityLevel.Low,
      persistence = Probability(0.5).toOption.get,
      footprint = Probability(0.3).toOption.get
    )
    Malware(
      name = "TestMalware",
      kind = MalwareKind.Worm,
      traits = traits,
      vectors = Set(PropagationVector.NetworkExploit)
    ).toOption.get

  private def validNode(
      defenseLevel: Double = 0.0,
      patchLevel: Double = 0.0,
      nodeType: NodeType = NodeType.Workstation,
      idSuffix: String = "test-node"
  ): Node =
    val id = NodeId.of(idSuffix).getOrElse(fail("Failed to create NodeId"))
    Node(id, nodeType, patchLevel, defenseLevel, NodeState.Healthy, workload = 0.0, Set())

  private def validEdge(
      source: Node,
      target: Node,
      channelType: Connection.ChannelType = Connection.ChannelType.LAN
  ): Connection.Edge =
    Connection.Edge(source, target, Connection.Channel.default(channelType), protocol = None)

  test(
    "infectionProbability with no defense/patch reduction still applies structural vulnerability"
  ):
    val malware = validMalware(infectivity = Probability(0.6).toOption.get)
    val source = validNode(idSuffix = "src", nodeType = NodeType.Workstation)
    val target = validNode(idSuffix = "dst", nodeType = NodeType.Workstation)
    val edge = validEdge(source, target)

    val result = ContagionRules.infectionProbability(malware, target, edge)

    result.value should be < 0.6

  test("infectionProbability combines defense, patch and structural vulnerability in order"):
    val malware = validMalware(infectivity = Probability(0.8).toOption.get)
    val source = validNode(idSuffix = "src")
    val target = validNode(
      idSuffix = "dst",
      defenseLevel = 0.25,
      patchLevel = 0.5,
      nodeType = NodeType.Workstation
    )
    val edge = validEdge(source, target)

    val result = ContagionRules.infectionProbability(malware, target, edge)

    result.value should be <= 0.3

  test("infectionProbability clamps at 1.0 instead of overflowing"):
    val malware = validMalware(infectivity = Probability(0.9).toOption.get)
    val source = validNode(idSuffix = "src")
    val target = validNode(idSuffix = "dst", nodeType = NodeType.IoTDevice)
    val edge = validEdge(source, target)

    val result = ContagionRules.infectionProbability(malware, target, edge)

    result.value should be <= 1.0

  test("infectionProbability stays within [0,1] for arbitrary inputs"):
    val malware = validMalware(infectivity = Probability(0.6).toOption.get)
    val source = validNode(idSuffix = "src")
    val target =
      validNode(idSuffix = "dst", defenseLevel = 0.3, patchLevel = 0.2, nodeType = NodeType.Router)
    val edge = validEdge(source, target)

    val result = ContagionRules.infectionProbability(malware, target, edge)

    result.value should (be >= 0.0 and be <= 1.0)

  test("resolveInfection returns true when the roll is below the computed probability"):
    val malware = validMalware(infectivity = Probability(0.9).toOption.get)
    val source = validNode(idSuffix = "src")
    val target = validNode(idSuffix = "dst", nodeType = NodeType.Router)
    val edge = validEdge(source, target)

    ContagionRules.resolveInfection(malware, target, edge, roll = 0.01) shouldBe true

  test("resolveInfection returns false when the roll is above the computed probability"):
    val malware = validMalware(infectivity = Probability(0.1).toOption.get)
    val source = validNode(idSuffix = "src")
    val target = validNode(
      idSuffix = "dst",
      defenseLevel = 0.9,
      patchLevel = 0.9,
      nodeType = NodeType.Workstation
    )
    val edge = validEdge(source, target)

    ContagionRules.resolveInfection(malware, target, edge, roll = 0.99) shouldBe false
