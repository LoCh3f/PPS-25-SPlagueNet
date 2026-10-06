package it.unibo.splague.update.messages

import it.unibo.splague.AppState
import it.unibo.splague.model.connection.Connection.{Channel, ChannelType, Edge}
import it.unibo.splague.model.malware.*
import it.unibo.splague.model.node.*
import it.unibo.splague.model.{ModelState, Probability, Scenario}
import it.unibo.splague.update.messages.ScenarioFormUpdate
import it.unibo.splague.update.simulation.SimulationState
import it.unibo.splague.update.simulation.event.SimulationEvents.{Event, EventSelector}
import it.unibo.splague.update.{Msg, TopologyShape}
import it.unibo.splague.view.form.{AwarenessForm, EdgeForm, NodeForm, ScenarioForm}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class ScenarioFormUpdateSuite extends AnyFunSuite with Matchers:

  private val validTraits = (for
    infectivity <- Probability(0.6)
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

  private val malware = Malware(
    name = "TestMalware",
    kind = MalwareKind.Worm,
    traits = validTraits,
    vectors = Set(PropagationVector.NetworkExploit)
  ).toOption.get

  private val node1 = Node(
    NodeId.of("n1").toOption.get,
    NodeType.Router,
    patchLevel = 0.2,
    defenseLevel = 0.5,
    state = NodeState.Healthy,
    workload = 0.3,
    Set()
  )

  private val node2 = Node(
    NodeId.of("n2").toOption.get,
    NodeType.Workstation,
    patchLevel = 0.0,
    defenseLevel = 0.0,
    state = NodeState.Healthy,
    workload = 0.0,
    Set()
  )

  private val edge = Edge(node1, node2, Channel.default(ChannelType.LAN), protocol = None)

  private val topology = Topology(
    nodes = Map(node1.nodeId.value -> node1, node2.nodeId.value -> node2),
    edges = Set(edge)
  )

  private val baseline = Scenario(
    name = "Baseline",
    topology = topology,
    virus = malware,
    startingNode = node1,
    tick = 0,
    seed = 7,
    maxIterations = 3
  ).toOption.get

  private val baseForm = ScenarioForm.fromScenario(baseline)

  private val noOpEvent: Event = (s: Scenario) => s
  private val noOpSelector: EventSelector = (_: Scenario) => noOpEvent

  private def stateWithForm(form: ScenarioForm = baseForm): AppState =
    AppState.init(ModelState()).copy(scenarioForm = Some(form))

  private def stateWithRunningSimulation(form: ScenarioForm = baseForm): AppState =
    stateWithForm(form).copy(
      simulation = Some(
        SimulationState(
          initial = baseline,
          selector = noOpSelector,
          states = LazyList(baseline),
          current = baseline,
          running = true
        )
      )
    )

  test("UpdateScenarioName updates name, seed, maxIterations and startingNodeId"):
    val editing = stateWithForm()
    val edited =
      baseForm.copy(name = "Renamed", seed = "42", maxIterations = "10", startingNodeId = "n2")

    val result = ScenarioFormUpdate.update(Msg.UpdateScenarioName(edited), editing)

    result.scenarioForm.map(_.name) shouldBe Some("Renamed")
    result.scenarioForm.map(_.seed) shouldBe Some("42")
    result.scenarioForm.map(_.maxIterations) shouldBe Some("10")
    result.scenarioForm.map(_.startingNodeId) shouldBe Some("n2")
    result.errors shouldBe Vector.empty

  test("UpdateScenarioName reports an error when no scenario form is open"):
    val state = AppState.init(ModelState())

    val result = ScenarioFormUpdate.update(Msg.UpdateScenarioName(baseForm), state)

    result.errors should not be Vector.empty

  test("AddNode appends a node to the topology"):
    val editing = stateWithForm()
    val newNode = NodeForm.fromNode(
      Node(NodeId.of("n3").toOption.get, NodeType.Server, 0.0, 0.0, NodeState.Healthy, 0.0, Set())
    )

    val result = ScenarioFormUpdate.update(Msg.AddNode(newNode), editing)

    result.scenarioForm.get.topology.nodes.map(_.id) should contain("n3")
    result.errors shouldBe Vector.empty

  test("UpdateNode replaces the node sharing its id"):
    val editing = stateWithForm()
    val updatedNode1 = NodeForm.fromNode(node1).copy(workload = "0.9")

    val result = ScenarioFormUpdate.update(Msg.UpdateNode(updatedNode1), editing)

    result.scenarioForm.get.topology.nodes
      .find(_.id == "n1")
      .map(_.workload) shouldBe Some("0.9")

  test("RemoveNode removes the node and any edge touching it"):
    val editing = stateWithForm()

    val result = ScenarioFormUpdate.update(Msg.RemoveNode("n1"), editing)

    result.scenarioForm.get.topology.nodes.map(_.id) should not contain "n1"
    // The only edge in the fixture touches n1, so it must be dropped along with the node.
    result.scenarioForm.get.topology.edges shouldBe Vector.empty

  test("AddEdge appends an edge to the topology"):
    val editing =
      stateWithForm(baseForm.copy(topology = baseForm.topology.copy(edges = Vector.empty)))
    val newEdge = EdgeForm.fromEdge(edge)

    val result = ScenarioFormUpdate.update(Msg.AddEdge(newEdge), editing)

    result.scenarioForm.get.topology.edges should contain(newEdge)

  test("UpdateEdge replaces the edge sharing its endpoints"):
    val editing = stateWithForm()
    val updatedEdge = EdgeForm.fromEdge(edge)
    val withNewBandwidth = updatedEdge.copy(channel = updatedEdge.channel.copy(bandwidth = "999"))

    val result = ScenarioFormUpdate.update(Msg.UpdateEdge(withNewBandwidth), editing)

    result.scenarioForm.get.topology.edges.map(_.channel.bandwidth) shouldBe Vector("999")

  test("RemoveEdge removes the matching edge"):
    val editing = stateWithForm()

    val result = ScenarioFormUpdate.update(Msg.RemoveEdge(EdgeForm.fromEdge(edge)), editing)

    result.scenarioForm.get.topology.edges shouldBe Vector.empty

  test("AddShape(Star) adds 5 namespaced nodes to the empty topology"):
    val emptyForm =
      baseForm.copy(topology = baseForm.topology.copy(nodes = Vector.empty, edges = Vector.empty))
    val editing = stateWithForm(emptyForm)

    val result = ScenarioFormUpdate.update(Msg.AddShape(TopologyShape.Star), editing)

    result.scenarioForm.get.topology.nodes.size shouldBe 5
    result.errors shouldBe Vector.empty

  test("AddShape increments the generation suffix to avoid id collisions on repeated clicks"):
    val emptyForm =
      baseForm.copy(topology = baseForm.topology.copy(nodes = Vector.empty, edges = Vector.empty))
    val editing = stateWithForm(emptyForm)

    val once = ScenarioFormUpdate.update(Msg.AddShape(TopologyShape.Ring), editing)
    val twice = ScenarioFormUpdate.update(Msg.AddShape(TopologyShape.Ring), once)

    val ids = twice.scenarioForm.get.topology.nodes.map(_.id).toSet
    ids should contain("ring1-0")
    ids should contain("ring2-0")
    twice.errors shouldBe Vector.empty

  test("AddShape is refused while the simulation is running"):
    val editing = stateWithRunningSimulation()

    val result = ScenarioFormUpdate.update(Msg.AddShape(TopologyShape.Mesh), editing)

    result.scenarioForm shouldBe editing.scenarioForm
    result.errors should not be Vector.empty

  test("AddShape reports an error when no scenario form is open"):
    val state = AppState.init(ModelState())

    val result = ScenarioFormUpdate.update(Msg.AddShape(TopologyShape.Star), state)

    result.errors should not be Vector.empty

  test("UpdateMalware replaces the scenario's virus"):
    val editing = stateWithForm()
    val newMalwareForm = baseForm.virus.copy(name = "NewVirus")

    val result = ScenarioFormUpdate.update(Msg.UpdateMalware(newMalwareForm), editing)

    result.scenarioForm.map(_.virus.name) shouldBe Some("NewVirus")
    result.errors shouldBe Vector.empty

  test("UpdateAwareness parses and updates the awareness value"):
    val editing = stateWithForm()

    val result = ScenarioFormUpdate.update(Msg.UpdateAwareness(AwarenessForm("0.5")), editing)

    result.scenarioForm.map(_.awareness) shouldBe Some(0.5)
    result.errors shouldBe Vector.empty

  test(
    "UpdateAwareness reports a parsing error for a non-numeric value, without touching the form"
  ):
    val editing = stateWithForm()

    val result =
      ScenarioFormUpdate.update(Msg.UpdateAwareness(AwarenessForm("not-a-number")), editing)

    result.scenarioForm shouldBe editing.scenarioForm
    result.errors should not be Vector.empty

  test("UpdateAwareness reports an error when no scenario form is open"):
    val state = AppState.init(ModelState())

    val result = ScenarioFormUpdate.update(Msg.UpdateAwareness(AwarenessForm("0.5")), state)

    result.errors should not be Vector.empty

  test("UpdateCountermeasure replaces the countermeasure configuration"):
    val editing = stateWithForm()
    val newConfig = baseForm.countermeasureConfig.copy(patchBoostAmount = "0.75")

    val result = ScenarioFormUpdate.update(Msg.UpdateCountermeasure(newConfig), editing)

    result.scenarioForm.map(_.countermeasureConfig.patchBoostAmount) shouldBe Some("0.75")
    result.errors shouldBe Vector.empty
