package csw.time.api

import java.time.Instant

trait TimeService {

  def UTCTime(): Instant

  def TAITime(): Instant
}
