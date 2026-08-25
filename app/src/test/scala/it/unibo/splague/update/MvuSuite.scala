package it.unibo.splague.update

import it.unibo.splague.update.Mvu.Screen.{Menu, Simulation}
import org.junit.runner.RunWith
import org.scalatest.{EitherValues, stats}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import it.unibo.splague.update.Mvu.{ModelState, Msg, Screen}
import scalaz.StreamT.Step

@RunWith(classOf[JUnitRunner])
final class MvuSuite extends AnyFunSuite with Matchers with EitherValues:
  test("Msg enum should contain all expected states"):
    val states = Msg.values
    states should contain(Msg.Step)

  test("Creating a ModelState with a specific Screen should have that Screen"):
    val model: ModelState = ModelState(screen = Simulation(None))

    model.screen shouldBe Simulation(None)

  test("ModelState.init should return a ModelState with screen == Menu"):
    val model: ModelState = ModelState.init()

    model.screen shouldBe Menu
