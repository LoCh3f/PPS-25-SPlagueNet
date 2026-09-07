package it.unibo.splague.update

import it.unibo.splague.model.Probability
import it.unibo.splague.model.malware.MalwareKind.Worm
import it.unibo.splague.model.malware.{
  Malware,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType}
import org.junit.runner.RunWith
import org.scalactic.Tolerance.convertNumericToPlusOrMinusWrapper
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DestructionRulesSuite extends AnyFunSuite:

  private val nodeId = NodeId.of("n1").getOrElse(fail())

  private val validTraits = (for
    infectivity <- Probability(0.5)
    stealth <- Probability(0.5)
    persistence <- Probability(0.5)
    footprint <- Probability(0.4)
  yield MalwareTraits(
    infectivity,
    stealth,
    payloadSeverity = PayloadSeverityLevel.Medium,
    persistence,
    footprint
  )).toOption.get

  private val dummyMalware = Malware(
    "test-virus",
    Worm,
    validTraits,
    Set(PropagationVector.NetworkExploit)
  ).getOrElse(fail())

  test("increaseWorkload increases node workload based on footprint and payload severity"):
    val node = Node(nodeId, NodeType.Workstation, 0.0, 0.5, NodeState.Infected, 0.2, Set())
    val newWorkload = DestructionRules.increaseWorkload(node, dummyMalware)
    // Medium severity factor is 0.2, footprint is 0.4 -> increment = 0.4 * 0.2 = 0.08
    newWorkload shouldBe 0.28 +- 1e-9

  test("increaseWorkload clamps workload at 1.0 maximum"):
    val node = Node(nodeId, NodeType.Workstation, 0.0, 0.5, NodeState.Infected, 0.95, Set())
    val newWorkload = DestructionRules.increaseWorkload(node, dummyMalware)
    newWorkload shouldBe 1.0

  test("destructionProbability equals node workload clamped"):
    val node = Node(nodeId, NodeType.Workstation, 0.0, 0.5, NodeState.Infected, 0.75, Set())
    val prob = DestructionRules.destructionProbability(node)
    prob.value shouldBe 0.75

  test("resolveDestruction returns true when roll is less than workload probability"):
    val node = Node(nodeId, NodeType.Workstation, 0.0, 0.5, NodeState.Infected, 0.6, Set())
    DestructionRules.resolveDestruction(node, 0.4) shouldBe true

  test(
    "resolveDestruction returns false when roll is greater than or equal to workload probability"
  ):
    val node = Node(nodeId, NodeType.Workstation, 0.0, 0.5, NodeState.Infected, 0.6, Set())
    DestructionRules.resolveDestruction(node, 0.6) shouldBe false
    DestructionRules.resolveDestruction(node, 0.8) shouldBe false
