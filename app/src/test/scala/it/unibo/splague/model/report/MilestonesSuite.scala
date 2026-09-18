package it.unibo.splague.model.report

import org.junit.runner.RunWith
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class MilestonesSuite extends AnyFunSuite with Matchers with EitherValues:

  private def tick(tick: Int, infected: Int, destroyed: Int = 0): TickSummary =
    TickSummary(
      tick = tick,
      healthy = 0,
      infected = infected,
      quarantined = 0,
      immune = 0,
      destroyed = destroyed,
      awareness = 0.0
    )

  test("firstSpreadTick is the first tick where infected count increases from the previous tick"):
    val timeline = Vector(
      tick(0, infected = 1),
      tick(1, infected = 1),
      tick(2, infected = 3),
      tick(3, infected = 3)
    )

    Milestones.from(timeline).firstSpreadTick shouldBe Some(2)

  test("firstSpreadTick is None when infected never increases after tick 0"):
    val timeline = Vector(tick(0, infected = 1), tick(1, infected = 1), tick(2, infected = 0))

    Milestones.from(timeline).firstSpreadTick shouldBe empty

  test(
    "peakInfectedTick and peakInfectedCount report the first tick reaching the maximum infected count"
  ):
    val timeline = Vector(
      tick(0, infected = 1),
      tick(1, infected = 4),
      tick(2, infected = 4),
      tick(3, infected = 2)
    )

    val milestones = Milestones.from(timeline)
    milestones.peakInfectedTick shouldBe 1
    milestones.peakInfectedCount shouldBe 4

  test("firstDestructionTick is the first tick with a destroyed node, if any"):
    val timeline = Vector(
      tick(0, infected = 1),
      tick(1, infected = 1, destroyed = 0),
      tick(2, infected = 1, destroyed = 2)
    )

    Milestones.from(timeline).firstDestructionTick shouldBe Some(2)

  test("firstDestructionTick is None when no node is ever destroyed"):
    val timeline = Vector(tick(0, infected = 1), tick(1, infected = 1))

    Milestones.from(timeline).firstDestructionTick shouldBe empty
