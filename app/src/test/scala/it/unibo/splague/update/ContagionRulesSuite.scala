package it.unibo.splague.update

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import it.unibo.splague.model.Probability
import it.unibo.splague.model.malware.{
  Malware,
  MalwareKind,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType}

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
      nodeType: NodeType = NodeType.Workstation
  ): Node =
    val id = NodeId.of("test-node").getOrElse(fail("Failed to create NodeId"))
    Node(id, nodeType, patchLevel, defenseLevel, NodeState.Healthy, workload = 0.0)

  test(
    "infectionProbability with no defense/patch reduction still applies structural vulnerability"
  ):
    val malware = validMalware(infectivity = Probability(0.6).toOption.get)
    val node = validNode(nodeType = NodeType.Workstation)
    val result = ContagionRules.infectionProbability(malware, node)
    result.value shouldBe 0.48 +- 0.0001

  test("infectionProbability combines defense, patch and structural vulnerability in order"):
    val malware = validMalware(infectivity = Probability(0.8).toOption.get)
    val node = validNode(defenseLevel = 0.25, patchLevel = 0.5, nodeType = NodeType.Workstation)
    val result = ContagionRules.infectionProbability(malware, node)
    result.value shouldBe 0.24 +- 0.0001

  test("infectionProbability clamps at 1.0 instead of overflowing"):
    val malware = validMalware(infectivity = Probability(0.9).toOption.get)
    val node = validNode(nodeType = NodeType.IoTDevice)
    val result = ContagionRules.infectionProbability(malware, node)
    result.value shouldBe 1.0

  test("infectionProbability stays within [0,1] for arbitrary inputs"):
    val malware = validMalware(infectivity = Probability(0.6).toOption.get)
    val node = validNode(defenseLevel = 0.3, patchLevel = 0.2, nodeType = NodeType.Router)
    val result = ContagionRules.infectionProbability(malware, node)
    result.value should (be >= 0.0 and be <= 1.0)

  test("resolveInfection returns true when the roll is below the computed probability"):
    val malware = validMalware(infectivity = Probability(0.9).toOption.get)
    val node = validNode(nodeType = NodeType.Router)
    ContagionRules.resolveInfection(malware, node, roll = 0.01) shouldBe true

  test("resolveInfection returns false when the roll is above the computed probability"):
    val malware = validMalware(infectivity = Probability(0.1).toOption.get)
    val node = validNode(defenseLevel = 0.9, patchLevel = 0.9, nodeType = NodeType.Workstation)
    ContagionRules.resolveInfection(malware, node, roll = 0.99) shouldBe false
