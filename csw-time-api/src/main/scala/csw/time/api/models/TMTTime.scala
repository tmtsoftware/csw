package csw.time.api.models

import java.time.{ZoneId, ZonedDateTime}

sealed trait TMTTime[T <: TMTTime[T]] { self: T =>
  val value: ZonedDateTime

  def at(zoneId: ZoneId): T
  def atLocal: T  = at(ZoneId.systemDefault())
  def atHawaii: T = at(ZoneId.of("US/Hawaii"))

  private[time] def atZone(zoneId: ZoneId) = value.toInstant.atZone(zoneId)
}

object TMTTime {
  case class UTCTime(value: ZonedDateTime) extends TMTTime[UTCTime] {
    override def at(zoneId: ZoneId): UTCTime = copy(atZone(zoneId))
  }

  case class TAITime(value: ZonedDateTime) extends TMTTime[TAITime] {
    override def at(zoneId: ZoneId): TAITime = copy(atZone(zoneId))
  }
}
