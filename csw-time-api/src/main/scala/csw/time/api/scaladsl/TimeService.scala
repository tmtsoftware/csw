package csw.time.api.scaladsl
import java.time.Instant

import csw.time.api.models.Cancellable
import csw.time.api.models.CswInstant.{TaiInstant, UtcInstant}

trait TimeService {

  def utcTime(): UtcInstant

  def taiTime(): TaiInstant

  def taiOffset(): Int

  def scheduleOnce(startTime: Instant)(task: => Unit): Cancellable
}
