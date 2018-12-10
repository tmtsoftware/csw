package csw.time.api.models

import java.time.ZonedDateTime

sealed trait TMTTime {
  val value: ZonedDateTime
}

object TMTTime {
  case class UTCTime(value: ZonedDateTime) extends TMTTime
  case class TAITime(value: ZonedDateTime) extends TMTTime
}
