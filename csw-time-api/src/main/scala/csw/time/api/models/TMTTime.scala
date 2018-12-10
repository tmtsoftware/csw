package csw.time.api.models

import java.time.{ZoneId, ZonedDateTime}

sealed trait TMTTime[T <: TMTTime[T]] { self: T =>
  val value: ZonedDateTime

  def at(zoneId: ZoneId): T
  def atLocal: T
  def atHawaii: T

  private[time] def atZone(zoneId: ZoneId) = value.toInstant.atZone(zoneId)

  private[time] def localZDT  = atZone(ZoneId.systemDefault())
  private[time] def hawaiiZDT = atZone(ZoneId.of("US/Hawaii"))
}

object TMTTime {
  case class UTCTime(value: ZonedDateTime) extends TMTTime[UTCTime] {
    override def at(zoneId: ZoneId): UTCTime = copy(atZone(zoneId))
    override def atLocal: UTCTime            = copy(localZDT)
    override def atHawaii: UTCTime           = copy(hawaiiZDT)
  }

  case class TAITime(value: ZonedDateTime) extends TMTTime[TAITime] {
    override def at(zoneId: ZoneId): TAITime = copy(atZone(zoneId))
    override def atLocal: TAITime            = copy(localZDT)
    override def atHawaii: TAITime           = copy(hawaiiZDT)
  }
}
