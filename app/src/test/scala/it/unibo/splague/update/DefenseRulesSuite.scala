package it.unibo.splague.update

import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.matchers.should.Matchers.*

@RunWith(classOf[JUnitRunner])
class DefenseRulesSuite extends AnyFunSuite:
  test("DefenseBoost increases the defense level of healthy nodes up to 1.0"):
    val node = Node(
      nodeId = NodeId.of("n1").fold(err => fail(s"Failed to create NodeId: $err"), identity),
      nodeType = NodeType.Workstation,
      patchLevel = 0.0,
      defenseLevel = 0.8,
      state = NodeState.Healthy,
      workload = 0.0,
      vectors = Set()
    )

    val boostedNode = DefenseRules.boostDefense(node)

    boostedNode.defenseLevel shouldBe 0.85 +- 0.0001

  test("DefenseBoost clamps defense level at 1.0"):
    val node = Node(
      nodeId = NodeId.of("n1").fold(err => fail(s"Failed to create NodeId: $err"), identity),
      nodeType = NodeType.Workstation,
      patchLevel = 0.0,
      defenseLevel = 0.9,
      state = NodeState.Healthy,
      workload = 0.0,
      vectors = Set()
    )

    val boostedNode = DefenseRules.boostDefense(node)
    boostedNode.defenseLevel shouldBe 0.95 +- 0.0001

  test("PatchBoost increases the patch level of healthy nodes up to 1.0"):
    val node = Node(
      nodeId = NodeId.of("n1").fold(err => fail(s"Failed to create NodeId: $err"), identity),
      nodeType = NodeType.Workstation,
      patchLevel = 0.0,
      defenseLevel = 0.8,
      state = NodeState.Healthy,
      workload = 0.0,
      vectors = Set()
    )

    val boostedNode = DefenseRules.boostPatch(node)

    boostedNode.patchLevel shouldBe 0.05 +- 0.0001

  test("PatchBoost clamps patch level at 1.0"):
    val node = Node(
      nodeId = NodeId.of("n1").fold(err => fail(s"Failed to create NodeId: $err"), identity),
      nodeType = NodeType.Workstation,
      patchLevel = 0.0,
      defenseLevel = 0.9,
      state = NodeState.Healthy,
      workload = 0.0,
      vectors = Set()
    )

    val boostedNode = DefenseRules.boostPatch(node)
    boostedNode.patchLevel shouldBe 0.05 +- 0.0001
