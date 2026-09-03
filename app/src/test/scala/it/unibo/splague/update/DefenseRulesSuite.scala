package it.unibo.splague.update

import it.unibo.splague.model.Probability
import it.unibo.splague.model.countermeasures.{CountermeasureConfig, Countermeasures}
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType}
import it.unibo.splague.update.DefenseRules.{cureProbability, resolveCure}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.matchers.should.Matchers.{shouldBe, *}

@RunWith(classOf[JUnitRunner])
class DefenseRulesSuite extends AnyFunSuite:
  private val nodeId = NodeId.of("n1").getOrElse(fail("Failed to create NodeId"))

  private def createNode(nodeType: NodeType, patchLevel: Double): Node =
    Node(
      nodeId = nodeId,
      nodeType = nodeType,
      patchLevel = patchLevel,
      defenseLevel = 0.0,
      state = NodeState.Infected,
      workload = 0.0,
      vectors = Set()
    )

  private val countermeasureConfig: CountermeasureConfig = CountermeasureConfig.empty

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

    val boostedNode = DefenseRules.boostDefense(node, countermeasureConfig)

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

    val boostedNode = DefenseRules.boostDefense(node, countermeasureConfig)
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

    val boostedNode = DefenseRules.boostPatch(node, countermeasureConfig)

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

    val boostedNode = DefenseRules.boostPatch(node, countermeasureConfig)
    boostedNode.patchLevel shouldBe 0.05 +- 0.0001

  test("cureProbability returns baseline chance when node is unpatched"):
    val node = createNode(NodeType.Workstation, 0.0)
    val probability = cureProbability(node, countermeasureConfig)

    probability.value shouldBe countermeasureConfig.patchCureProbability

  test("cureProbability approaches certainty when node is fully patched"):
    val node = createNode(NodeType.Workstation, 1.0)
    val probability = cureProbability(node, countermeasureConfig)

    probability.value shouldBe 1.0 +- 1e-9

  test("cureProbability scales linearly for partially patched nodes"):
    val node = createNode(NodeType.Workstation, 0.5)
    val probability = cureProbability(node, countermeasureConfig)
    val expected =
      countermeasureConfig.patchCureProbability + (1.0 - countermeasureConfig.patchCureProbability) * 0.5

    probability.value shouldBe expected +- 1e-9

  test("cureProbability returns zero when Patch countermeasure is not applicable to node type"):
    // IoTDevice device doesn't support patch
    val node = createNode(NodeType.IoTDevice, 1.0)
    cureProbability(node, countermeasureConfig).value shouldBe 0.0

  test("solveCure returns true when roll is strictly less than probability"):
    val prob = Probability.clamped(0.7)
    resolveCure(prob, 0.5) shouldBe true

  test("solveCure returns false when roll is greater than or equal to probability"):
    val prob = Probability.clamped(0.7)
    resolveCure(prob, 0.7) shouldBe false
    resolveCure(prob, 0.9) shouldBe false
