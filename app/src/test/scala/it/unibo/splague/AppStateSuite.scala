package it.unibo.splague

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class AppStateSuite extends AnyFunSuite with Matchers:

  test("initialModel preloads both built-in scenarios, by name"):
    val model = AppState.initialModel

    model.scenarios.map(_.name) should contain allOf ("Linear chain", "Complex enterprise mesh")

  test("initialModel preloads both built-in scenarios' malware, by name"):
    val model = AppState.initialModel

    model.malwares.map(_.name) should contain allOf ("SimpleWorm", "SimpleDropper")

  test("initialModel starts with no scenario selected"):
    AppState.initialModel.currentScenario shouldBe None
