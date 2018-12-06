package csw.time.api.models

import java.time.{Instant, ZoneId, ZonedDateTime}

sealed trait CswInstant {
  val value: Instant

  def atZone(zoneId: ZoneId): ZonedDateTime = value.atZone(zoneId)
  def atLocal: ZonedDateTime                = atZone(ZoneId.systemDefault())
  def atHawaii: ZonedDateTime               = atZone(ZoneId.of("US/Hawaii"))
}

object CswInstant {
  case class UtcInstant(value: Instant) extends CswInstant
  case class TaiInstant(value: Instant) extends CswInstant
}
