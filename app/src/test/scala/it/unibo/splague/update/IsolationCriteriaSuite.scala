package it.unibo.splague.update

import it.unibo.splague.model.node.NodeId
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith
import it.unibo.splague.model.node.{Node, NodeId, NodeState, NodeType, Topology}

@RunWith(classOf[JUnitRunner])
class IsolationCriteriaSuite extends AnyFunSuite:
  private val id1 = NodeId.of("n1").getOrElse(fail())
  private val id2 = NodeId.of("n2").getOrElse(fail())

  private val workstationNode =
    Node(id1, NodeType.Workstation, 0.2, 0.8, NodeState.Healthy, 0.5, Set())
  private val serverNode = Node(id2, NodeType.Server, 0.9, 0.1, NodeState.Infected, 0.1, Set())

  test("IsolationCriteria.all matches any node"):
    val criteria = IsolationCriteria.all
    criteria.matches(workstationNode) shouldBe true
    criteria.matches(serverNode) shouldBe true

  test("IsolationCriteria.byType matches nodes of specified types only"):
    val criteria = IsolationCriteria.byType(Set(NodeType.Workstation))
    criteria.matches(workstationNode) shouldBe true
    criteria.matches(serverNode) shouldBe false

  test("IsolationCriteria.byMinWorkload matches nodes with workload at or above threshold"):
    val criteria = IsolationCriteria.byMinWorkload(0.5)
    // workstationNode has workload = 0.5
    criteria.matches(workstationNode) shouldBe true
    // serverNode has workload = 0.1
    criteria.matches(serverNode) shouldBe false

  test("IsolationCriteria.byMaxDefense matches nodes with defense level at or below threshold"):
    val criteria = IsolationCriteria.byMaxDefense(0.2)
    // workstationNode has defense = 0.8
    criteria.matches(workstationNode) shouldBe false
    // serverNode has defense = 0.1
    criteria.matches(serverNode) shouldBe true

  test("Combinator and requires both criteria to match"):
    val criteria =
      IsolationCriteria.byType(Set(NodeType.Server)).and(IsolationCriteria.byMinWorkload(0.0))
    criteria.matches(serverNode) shouldBe true
    criteria.matches(workstationNode) shouldBe false // Wrong type

  test("Combinator or requires at least one criterion to match"):
    val criteria =
      IsolationCriteria.byType(Set(NodeType.Workstation)).or(IsolationCriteria.byMaxDefense(0.9))
    criteria.matches(workstationNode) shouldBe true // Matches type
    criteria.matches(serverNode) shouldBe true // Matches defense (0.1 <= 0.9)
