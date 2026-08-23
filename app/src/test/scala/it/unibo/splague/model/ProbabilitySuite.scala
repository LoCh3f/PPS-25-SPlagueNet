package it.unibo.splague.model

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ProbabilitySuite extends AnyFunSuite:

  test("valid probability in [0,1] is accepted"):
    assert(Probability(0.5).isRight)
    assert(Probability(0.0).isRight)
    assert(Probability(1.0).isRight)

  test("value below 0 is rejected"):
    assert(Probability(-0.1).isLeft)

  test("value above 1 is rejected"):
    assert(Probability(1.1).isLeft)
