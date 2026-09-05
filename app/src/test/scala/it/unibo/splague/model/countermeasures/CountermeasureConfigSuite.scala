package it.unibo.splague.model.countermeasures

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.not.include
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CountermeasureConfigSuite extends AnyFunSuite:

  test("CountermeasureConfig creation succeeds with valid thresholds"):
    val validLevels = Map(0.5 -> Countermeasures.DefenseBoost)
    val result = CountermeasureConfig(Set(Countermeasures.DefenseBoost), validLevels)

    result.isRight shouldBe true

  test("CountermeasureConfig creation fails when threshold is negative"):
    val invalidLevels = Map(-0.1 -> Countermeasures.DefenseBoost)
    val result = CountermeasureConfig(Set.empty, invalidLevels)

    result.isLeft shouldBe true
