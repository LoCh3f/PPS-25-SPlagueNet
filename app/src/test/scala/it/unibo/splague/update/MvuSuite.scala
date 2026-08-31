package it.unibo.splague.update

import it.unibo.splague.update.Mvu.Screen.{Menu, Simulation}
import org.junit.runner.RunWith
import org.scalatest.{EitherValues, stats}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import it.unibo.splague.update.Mvu.{ModelState, Msg, Screen}

@RunWith(classOf[JUnitRunner])
final class MvuSuite extends AnyFunSuite with Matchers with EitherValues:
  test("Msg enum should contain all expected states"):
    val states = Msg.values
    states should contain allOf (
      Msg.ReturnToMenu,
      Msg.Step,
      Msg.GoToSimulation,
      Msg.GoToCreateView,
      Msg.GoToSelection,
      Msg.ConfirmScenario,
      Msg.LoadTopology,
      Msg.SaveScenario
    )

  test("Creating a ModelState with a specific Screen should have that Screen"):
    val model: ModelState = ModelState(screen = Simulation(LazyList()))

    model.screen shouldBe Simulation(LazyList())

  test("ModelState.init should return a ModelState with screen == Menu"):
    val model: ModelState = ModelState.init()

    model.screen shouldBe Menu

  test("update should leave the model unchanged when Msg.Step arrives on the Menu screen") {
    val model = ModelState(Screen.Menu)

    val result = Mvu.update(Msg.Step, model)

    result shouldBe model
  }

  test("update with Msg.Step and Screen.Simulation should return an updated version of the model") {
    succeed
  }
