package it.unibo.splague.view.simulation

import it.unibo.splague.model.connection.Connection.ChannelType
import it.unibo.splague.model.node.{NodeState, NodeType}
import it.unibo.splague.view.form.{ChannelForm, EdgeForm, NodeForm}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class WorkspaceGeometrySuite extends AnyFunSuite:

  private def nodeView(id: String, x: Double, y: Double): ViewNode =
    ViewNode(
      form = NodeForm(
        id = id,
        nodeType = NodeType.Workstation,
        patchLevel = "0.0",
        defenseLevel = "0.0",
        state = NodeState.Healthy,
        workload = "0.0",
        vectors = Set.empty
      ),
      x = x,
      y = y
    )

  private val channel =
    ChannelForm(
      ChannelType.LAN,
      bandwidth = "100.0",
      latency = "1.0",
      jitter = "0.1",
      packetLoss = "0.0"
    )

  private def edge(from: String, to: String): EdgeForm =
    EdgeForm(from = from, to = to, channel = channel, protocol = None)

  test("screenToModel converts screen coordinates back to model space using pan offset and zoom"):
    WorkspaceGeometry.screenToModel(
      x = 110,
      y = 220,
      offsetX = 10,
      offsetY = 20,
      zoom = 2.0
    ) shouldBe (50.0, 100.0)

  test("screenToModel is the identity when there is no pan and zoom is 1.0"):
    WorkspaceGeometry.screenToModel(x = 42, y = 7, offsetX = 0, offsetY = 0, zoom = 1.0) shouldBe (
      42.0,
      7.0
    )

  test("pickNode returns the node whose center is within nodeRadius of the given point"):
    val views = Map("a" -> nodeView("a", 0.0, 0.0), "b" -> nodeView("b", 100.0, 100.0))

    WorkspaceGeometry.pickNode(
      x = 5.0,
      y = 0.0,
      nodeViews = views,
      nodeRadius = 18.0
    ) shouldBe Some("a")

  test("pickNode returns None when no node is within nodeRadius of the given point"):
    val views = Map("a" -> nodeView("a", 0.0, 0.0))

    WorkspaceGeometry.pickNode(
      x = 100.0,
      y = 100.0,
      nodeViews = views,
      nodeRadius = 18.0
    ) shouldBe None

  test("pickEdge returns the (from, to) pair of an edge whose segment is within tolerance"):
    val views = Map("a" -> nodeView("a", 0.0, 0.0), "b" -> nodeView("b", 100.0, 0.0))
    val edges = Vector(edge("a", "b"))

    WorkspaceGeometry.pickEdge(x = 50.0, y = 2.0, edges = edges, nodeViews = views) shouldBe Some(
      ("a", "b")
    )

  test("pickEdge returns None when the point is farther than the tolerance from every edge"):
    val views = Map("a" -> nodeView("a", 0.0, 0.0), "b" -> nodeView("b", 100.0, 0.0))
    val edges = Vector(edge("a", "b"))

    WorkspaceGeometry.pickEdge(x = 50.0, y = 50.0, edges = edges, nodeViews = views) shouldBe None

  test("pickEdge ignores an edge whose endpoints have no matching node view"):
    val views = Map("a" -> nodeView("a", 0.0, 0.0))
    val edges = Vector(edge("a", "missing"))

    WorkspaceGeometry.pickEdge(x = 0.0, y = 0.0, edges = edges, nodeViews = views) shouldBe None

  test("connectionExists is true for an edge in the given direction"):
    WorkspaceGeometry.connectionExists(Vector(edge("a", "b")), from = "a", to = "b") shouldBe true

  test("connectionExists is true regardless of direction, since it checks both ways"):
    WorkspaceGeometry.connectionExists(Vector(edge("a", "b")), from = "b", to = "a") shouldBe true

  test("connectionExists is false when no edge connects the two nodes"):
    WorkspaceGeometry.connectionExists(Vector(edge("a", "b")), from = "a", to = "c") shouldBe false
