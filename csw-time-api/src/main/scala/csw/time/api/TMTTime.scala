package csw.time.api

import java.time.{Instant, ZoneId, ZonedDateTime}

import csw.time.api.models.internal.TMTClock.clock
import julienrf.json.derived
import play.api.libs.json._

sealed trait TMTTime extends Product with Serializable {
  def value: Instant
}

object TMTTime {
  implicit val format: OFormat[TMTTime] = derived.flat.oformat((__ \ "type").format[String])
}

final case class UTCTime(value: Instant) extends TMTTime {
  def toTAI: TAITime = TAITime(value.plusSeconds(clock.offset))

  def at(zoneId: ZoneId): ZonedDateTime = value.atZone(zoneId)
  def atLocal: ZonedDateTime            = at(ZoneId.systemDefault())
  def atHawaii: ZonedDateTime           = at(ZoneId.of("US/Hawaii"))
}

object UTCTime {
  def now(): UTCTime = UTCTime(clock.utcInstant)
}

final case class TAITime(value: Instant) extends TMTTime {
  def toUTC: UTCTime = UTCTime(value.minusSeconds(clock.offset))
}

object TAITime {
  def now(): TAITime = TAITime(clock.taiInstant)
  def offset: Int    = clock.offset

  // fixme: only for testing and making it private[api] does not work from java test
  def setOffset(offset: Int): Unit = clock.setTaiOffset(offset)
}
