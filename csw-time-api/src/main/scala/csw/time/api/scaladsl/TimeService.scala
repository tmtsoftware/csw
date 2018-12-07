package csw.time.api.scaladsl
import java.time.Duration

import csw.time.api.models.Cancellable
import csw.time.api.models.CswInstant.{TaiInstant, UtcInstant}

trait TimeService {

  def utcTime(): UtcInstant

  def taiTime(): TaiInstant

  def toUtc(taiInstant: TaiInstant): UtcInstant

  def toTai(utcInstant: UtcInstant): TaiInstant

  def scheduleOnce(startTime: TaiInstant)(task: Runnable): Cancellable

  def schedulePeriodically(duration: Duration)(task: Runnable): Cancellable

  def schedulePeriodically(startTime: TaiInstant, duration: Duration)(task: Runnable): Cancellable

  private[time] def setTaiOffset(offset: Int): Unit
}
