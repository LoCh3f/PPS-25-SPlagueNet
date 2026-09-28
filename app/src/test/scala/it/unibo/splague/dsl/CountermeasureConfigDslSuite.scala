package it.unibo.splague.dsl

import it.unibo.splague.model.countermeasures.Countermeasures
import it.unibo.splague.update.{FirewallPolicy, IsolationCriteria}
import org.junit.runner.RunWith
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class CountermeasureConfigDslSuite extends AnyFunSuite with Matchers with EitherValues:
  test("builds a complete valid configuration"):
    val customFirewall = FirewallPolicy()
    val customCriteria = IsolationCriteria.all

    val result = countermeasureConfig:
      active(Countermeasures.Patch, Countermeasures.Firewall)
      0.3 triggers Countermeasures.Patch
      0.8 triggers Countermeasures.Firewall
      patchBoostAmount(0.1)
      defenseBoostAmount(0.2)
      patchCureProbability(0.7)
      isolationCriteria(customCriteria)
      firewallPolicy(customFirewall)

    val config = result.value
    config.activeCountermeasures should contain theSameElementsAs Set(
      Countermeasures.Patch,
      Countermeasures.Firewall
    )
    config.countermeasureLevels shouldBe Map(
      0.3 -> Countermeasures.Patch,
      0.8 -> Countermeasures.Firewall
    )
    config.patchBoostAmount shouldBe 0.1
    config.defenseBoostAmount shouldBe 0.2
    config.patchCureProbability shouldBe 0.7
    config.isolationCriteria shouldBe customCriteria
    config.firewallPolicy shouldBe customFirewall

  test("accumulates multiple calls to active(...)"):
    val result = countermeasureConfig:
      active(Countermeasures.Patch)
      active(Countermeasures.Firewall, Countermeasures.Isolation)

    result.value.activeCountermeasures should contain theSameElementsAs Set(
      Countermeasures.Patch,
      Countermeasures.Firewall,
      Countermeasures.Isolation
    )

  test("latest declaration overrides previous ones for single scalar values"):
    val result = countermeasureConfig:
      patchBoostAmount(0.1)
      patchBoostAmount(0.5) // This should win
      patchCureProbability(0.3)
      patchCureProbability(0.9) // This should win

    result.value.patchBoostAmount shouldBe 0.5
    result.value.patchCureProbability shouldBe 0.9

  test("overwrites trigger if same threshold is defined multiple times"):
    val result = countermeasureConfig:
      0.5 triggers Countermeasures.Isolation
      0.5 triggers Countermeasures.Patch // This should win

    result.value.countermeasureLevels.get(0.5) shouldBe Some(Countermeasures.Patch)
    result.value.countermeasureLevels should have size 1

  test("fails validation if a threshold is less than 0.0"):
    val result = countermeasureConfig:
      -0.1 triggers Countermeasures.Isolation

    result.left.value should contain(
      "Thresholds in countermeasureLevels must be between 0.0 and 1.0"
    )

  test("fails validation if a threshold is greater than 1.0"):
    val result = countermeasureConfig:
      1.5 triggers Countermeasures.Patch

    result.left.value should contain(
      "Thresholds in countermeasureLevels must be between 0.0 and 1.0"
    )

  test("fails validation if patchBoostAmount is out of bounds"):
    val resultNegative = countermeasureConfig:
      patchBoostAmount(-0.05)
    resultNegative.left.value should contain(
      "Boost amounts and cure probability must be between 0.0 and 1.0"
    )

    val resultOversized = countermeasureConfig:
      patchBoostAmount(1.1)
    resultOversized.left.value should contain(
      "Boost amounts and cure probability must be between 0.0 and 1.0"
    )

  test("fails validation if defenseBoostAmount is out of bounds"):
    val result = countermeasureConfig:
      defenseBoostAmount(2.0)

    result.left.value should contain(
      "Boost amounts and cure probability must be between 0.0 and 1.0"
    )

  test("fails validation if patchCureProbability is out of bounds"):
    val result = countermeasureConfig:
      patchCureProbability(-0.5)

    result.left.value should contain(
      "Boost amounts and cure probability must be between 0.0 and 1.0"
    )
