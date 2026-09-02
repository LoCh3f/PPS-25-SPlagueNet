package it.unibo.splague.model.countermeasures

import it.unibo.splague.model.countermeasures.Countermeasures.{
  DefenseBoost,
  Firewall,
  Isolation,
  Patch
}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.should
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CountermeasuresSuite extends AnyFunSuite:
  test("Countermeasures should contain all of the specified countermeasures"):
    val countermeasures = Countermeasures.values
    countermeasures should contain allOf (
      DefenseBoost,
      Firewall,
      Isolation,
      Patch
    )
