package it.unibo.splague.model.report

final case class Milestones(
    firstSpreadTick: Option[Int],
    peakInfectedTick: Int,
    peakInfectedCount: Int,
    firstDestructionTick: Option[Int]
)

object Milestones:
  def from(timeline: Vector[TickSummary]): Milestones =
    val firstSpreadTick =
      timeline.sliding(2).collectFirst {
        case Vector(previous, current) if current.infected > previous.infected => current.tick
      }

    val peak = timeline.maxBy(_.infected)

    val firstDestructionTick =
      timeline.find(_.destroyed > 0).map(_.tick)

    Milestones(
      firstSpreadTick = firstSpreadTick,
      peakInfectedTick = peak.tick,
      peakInfectedCount = peak.infected,
      firstDestructionTick = firstDestructionTick
    )
