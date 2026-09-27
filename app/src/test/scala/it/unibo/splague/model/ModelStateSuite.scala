package it.unibo.splague.model

import it.unibo.splague.model.malware.MalwareKind.Worm
import it.unibo.splague.model.malware.{
  Malware,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.*
import org.junit.runner.RunWith
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class ModelStateSuite extends AnyFunSuite with Matchers with EitherValues:

  private val node =
    Node(
      NodeId.of("node-01").getOrElse(fail()),
      NodeType.Router,
      0.1,
      0.2,
      NodeState.Healthy,
      0.0,
      Set()
    )

  private val topology = Topology(
    nodes = Map("node-01" -> node),
    edges = Set.empty
  )

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

  private val dummyVirus =
    Malware(
      "dummy",
      Worm,
      validTraits,
      vectors = Set(PropagationVector.NetworkExploit)
    ).toOption.get

  private def scenario(name: String): Scenario =
    Scenario(
      name = name,
      topology = topology,
      virus = dummyVirus,
      startingNode = node,
      tick = 0,
      seed = 42,
      maxIterations = 100
    ).getOrElse(fail())

  private val scenarioA = scenario("Scenario A")
  private val scenarioB = scenario("Scenario B")

  test("a freshly created ModelState is empty"):
    val state = ModelState()

    state.scenarios shouldBe empty
    state.malwares shouldBe empty
    state.currentScenario shouldBe None

  test("addScenario appends the scenario, keeping the previous ones"):
    val state = ModelState().addScenario(scenarioA).addScenario(scenarioB)

    state.scenarios shouldBe Vector(scenarioA, scenarioB)

  test("addMalware appends the malware, keeping the previous ones"):
    val state = ModelState().addMalware(dummyVirus)

    state.malwares shouldBe Vector(dummyVirus)

  test("upsertScenario appends the scenario when no existing one shares its name"):
    val state = ModelState().upsertScenario(scenarioA)

    state.scenarios shouldBe Vector(scenarioA)

  test("upsertScenario replaces the existing scenario with the same name instead of duplicating"):
    val editedScenarioA = scenario("Scenario A").copy(seed = 99)

    val state =
      ModelState().addScenario(scenarioA).addScenario(scenarioB).upsertScenario(editedScenarioA)

    state.scenarios shouldBe Vector(editedScenarioA, scenarioB)

  test("upsertMalware appends the malware when no existing one shares its name"):
    val state = ModelState().upsertMalware(dummyVirus)

    state.malwares shouldBe Vector(dummyVirus)

  test("upsertMalware replaces the existing malware with the same name instead of duplicating"):
    val editedVirus =
      Malware(
        "dummy",
        Worm,
        validTraits.copy(payloadSeverity = PayloadSeverityLevel.High),
        vectors = Set(PropagationVector.NetworkExploit)
      ).toOption.get

    val state = ModelState().addMalware(dummyVirus).upsertMalware(editedVirus)

    state.malwares shouldBe Vector(editedVirus)

  test("upsertScenario given previousName replaces the previously-named entry with the rename"):
    val renamed = scenario("Scenario A renamed")

    val state = ModelState()
      .addScenario(scenarioA)
      .addScenario(scenarioB)
      .upsertScenario(renamed, previousName = Some(scenarioA.name))

    // "Scenario A" is dropped and the rename is appended, so scenarioB (never touched) is first.
    state.scenarios shouldBe Vector(scenarioB, renamed)

  test("upsertScenario ignores previousName when it already equals the scenario's own name"):
    val editedScenarioA = scenario("Scenario A").copy(seed = 99)

    val state = ModelState()
      .addScenario(scenarioA)
      .upsertScenario(editedScenarioA, previousName = Some(scenarioA.name))

    state.scenarios shouldBe Vector(editedScenarioA)

  test("upsertMalware given previousName replaces the previously-named entry with the rename"):
    val renamedVirus =
      Malware(
        "renamed",
        Worm,
        validTraits,
        vectors = Set(PropagationVector.NetworkExploit)
      ).toOption.get

    val state = ModelState()
      .addMalware(dummyVirus)
      .upsertMalware(renamedVirus, previousName = Some(dummyVirus.name))

    state.malwares shouldBe Vector(renamedVirus)

  test("selectScenario selects a scenario that is present in the model state"):
    val state = ModelState().addScenario(scenarioA)

    val result = state.selectScenario(scenarioA)

    result.value.currentScenario shouldBe Some(scenarioA)

  test("selectScenario fails when the scenario is not present in the model state"):
    val state = ModelState().addScenario(scenarioA)

    val result = state.selectScenario(scenarioB)

    result shouldBe a[Left[?, ?]]
    result.left.value should include("not present")

  test("selectScenarioByName selects the scenario matching the given name"):
    val state = ModelState().addScenario(scenarioA).addScenario(scenarioB)

    val result = state.selectScenarioByName("Scenario B")

    result.value.currentScenario shouldBe Some(scenarioB)

  test("selectScenarioByName fails with a descriptive message when no scenario matches"):
    val state = ModelState().addScenario(scenarioA)

    val result = state.selectScenarioByName("missing")

    result shouldBe a[Left[?, ?]]
    result.left.value should include("missing")

  test("clearCurrentScenario resets the current scenario to None"):
    val state = ModelState().addScenario(scenarioA).selectScenario(scenarioA).value

    state.clearCurrentScenario.currentScenario shouldBe None

  test("clearCurrentScenario leaves scenarios and malwares untouched"):
    val state =
      ModelState().addScenario(scenarioA).addMalware(dummyVirus).selectScenario(scenarioA).value

    val cleared = state.clearCurrentScenario

    cleared.scenarios shouldBe state.scenarios
    cleared.malwares shouldBe state.malwares
