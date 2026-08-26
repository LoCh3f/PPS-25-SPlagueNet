package it.unibo.splague.update

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import it.unibo.splague.model.Probability
import it.unibo.splague.model.{Node, NodeId, NodeState, NodeType}
import it.unibo.splague.model.malware.{
  Malware,
  MalwareKind,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}

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
      nodeType: NodeType = NodeType.Router
  ): Node =
    val id = NodeId.of("test-node").getOrElse(fail("Failed to create NodeId"))
    Node(id, nodeType, patchLevel, defenseLevel, NodeState.Healthy, workload = 0.0)

  test("infectionProbability equals the malware's infectivity trait"):
    val malware = validMalware(infectivity = Probability(0.6).toOption.get)
    ContagionRules.infectionProbability(malware).value shouldBe 0.6

  test("withStructuralVulnerability scales base probability up for vulnerable node types"):
    val base = Probability(0.5).toOption.get
    val result = ContagionRules.withStructuralVulnerability(base, NodeType.IoTDevice) // 1.3
    result.value shouldBe 0.65 +- 0.0001

  test("withDefense reduces probability proportionally to defenseLevel"):
    val base = Probability(0.8).toOption.get
    val node = validNode(defenseLevel = 0.25)
    ContagionRules.withDefense(base, node).value shouldBe 0.6 +- 0.0001

  test("withStructuralVulnerability clamps at 1.0 instead of overflowing"):
    val base = Probability(0.9).toOption.get
    val result =
      ContagionRules.withStructuralVulnerability(base, NodeType.IoTDevice) // 0.9 * 1.3 = 1.17
    result.value shouldBe 1.0
