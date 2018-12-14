package csw.time.api.models

import java.time.{Instant, ZoneId, ZonedDateTime}

sealed trait TMTTime {
  def value: Instant
}

object TMTTime {
  case class UTCTime(value: Instant) extends TMTTime {

    def at(zoneId: ZoneId): ZonedDateTime = value.atZone(zoneId)

    def atLocal: ZonedDateTime = at(ZoneId.systemDefault())

    def atHawaii: ZonedDateTime = at(ZoneId.of("US/Hawaii"))
  }

  case class TAITime(value: Instant) extends TMTTime
}
