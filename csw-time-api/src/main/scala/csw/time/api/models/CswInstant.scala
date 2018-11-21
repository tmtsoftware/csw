package csw.time.api.models

import java.time.Instant

sealed trait CswInstant {
  val value: Instant
}

object CswInstant {
  case class UtcInstant(value: Instant) extends CswInstant
  case class TaiInstant(value: Instant) extends CswInstant
}
