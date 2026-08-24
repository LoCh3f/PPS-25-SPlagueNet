package it.unibo.splague.model

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class ProbabilitySuite extends AnyFunSuite with Matchers with EitherValues:

  test("valid probability in [0,1] is accepted"):
    Probability(0.5) shouldBe a[Right[?, ?]]
    Probability(0.0) shouldBe a[Right[?, ?]]
    Probability(1.0) shouldBe a[Right[?, ?]]

  test("value below 0 is rejected"):
    Probability(-0.1) shouldBe a[Left[?, ?]]

  test("value above 1 is rejected"):
    Probability(1.1) shouldBe a[Left[?, ?]]

  test("out-of-range value is rejected with a descriptive message"):
    Probability(-0.1).left.value should include("must be in [0,1]")
    Probability(1.1).left.value should include("must be in [0,1]")
