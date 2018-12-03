package csw.time.api.javadsl
import java.util.function.Consumer

import csw.time.api.models.Cancellable
import csw.time.api.models.CswInstant.{TaiInstant, UtcInstant}

trait ITimeService {
  def utcTime(): UtcInstant

  def taiTime(): TaiInstant

  def taiOffset(): Int

  def scheduleOnce(startTime: TaiInstant, task: Consumer[Void]): Cancellable
}
