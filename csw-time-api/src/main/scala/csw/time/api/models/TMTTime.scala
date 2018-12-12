package csw.time.api.models

import java.time.{ZoneId, ZonedDateTime}

sealed abstract class TMTTime[T <: TMTTime[T]] {
  def value: ZonedDateTime
  def copy(value: ZonedDateTime): T

  def at(zoneId: ZoneId): T = copy(atZone(zoneId))
  def atLocal: T            = copy(localZDT)
  def atHawaii: T           = copy(hawaiiZDT)

  private[time] def atZone(zoneId: ZoneId) = value.toInstant.atZone(zoneId)

  private[time] def localZDT  = atZone(ZoneId.systemDefault())
  private[time] def hawaiiZDT = atZone(ZoneId.of("US/Hawaii"))
}

object TMTTime {
  case class UTCTime(value: ZonedDateTime) extends TMTTime[UTCTime] {
    override def copy(value: ZonedDateTime): UTCTime = UTCTime(value)
  }
  case class TAITime(value: ZonedDateTime) extends TMTTime[TAITime] {
    override def copy(value: ZonedDateTime): TAITime = TAITime(value)
  }
}
