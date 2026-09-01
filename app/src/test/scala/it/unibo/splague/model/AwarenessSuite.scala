package it.unibo.splague.model

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class AwarenessSuite extends AnyFunSuite with Matchers with EitherValues:

  test("valid awareness in [0,1] is accepted"):
    Awareness(0.5) shouldBe a[Right[?, ?]]
    Awareness(0.0) shouldBe a[Right[?, ?]]
    Awareness(1.0) shouldBe a[Right[?, ?]]

  test("value below 0 is rejected"):
    Awareness(-0.1) shouldBe a[Left[?, ?]]

  test("value above 1 is rejected"):
    Awareness(1.1) shouldBe a[Left[?, ?]]

  test("out-of-range value is rejected with a descriptive message"):
    Awareness(-0.1).left.value should include("must be in [0,1]")
    Awareness(1.1).left.value should include("must be in [0,1]")

  test("none should be zero awareness"):
    Awareness.none.value shouldBe 0.0

  test("clamped should cap values above 1.0 down to 1.0"):
    Awareness.clamped(1.5).value shouldBe 1.0

  test("clamped should floor values below 0.0 up to 0.0"):
    Awareness.clamped(-0.5).value shouldBe 0.0

  test("clamped should leave in-range values untouched"):
    Awareness.clamped(0.42).value shouldBe 0.42 +- 0.0001

  test("raise should increase awareness by delta, clamped to [0,1]"):
    val base = Awareness.clamped(0.3)
    base.raise(0.2).value shouldBe 0.5 +- 0.0001

  test("raise should clamp at 1.0 instead of overflowing"):
    val base = Awareness.clamped(0.9)
    base.raise(0.5).value shouldBe 1.0

  test("raise should clamp at 0.0 when delta is negative and large"):
    val base = Awareness.clamped(0.2)
    base.raise(-0.5).value shouldBe 0.0
