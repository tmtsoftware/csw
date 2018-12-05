package csw.time.api.javadsl

import csw.time.api.models.Cancellable
import csw.time.api.models.CswInstant.{TaiInstant, UtcInstant}

trait ITimeService {
  def utcTime(): UtcInstant

  def taiTime(): TaiInstant

  def taiOffset(): Int

  def scheduleOnce(startTime: TaiInstant, task: Runnable): Cancellable

  private[time] def setTaiOffset(offset: Int): Unit
}
