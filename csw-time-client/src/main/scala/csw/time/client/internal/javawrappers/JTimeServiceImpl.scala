package csw.time.client.internal.javawrappers

import csw.time.api.javadsl.ITimeService
import csw.time.api.models.{Cancellable, CswInstant}
import csw.time.api.scaladsl.TimeService

class JTimeServiceImpl(timeService: TimeService) extends ITimeService {

  override def utcTime(): CswInstant.UtcInstant = timeService.utcTime()
  override def taiTime(): CswInstant.TaiInstant = timeService.taiTime()
  override def taiOffset(): Int                 = timeService.taiOffset()

  override def scheduleOnce(startTime: CswInstant.TaiInstant, task: Runnable): Cancellable =
    timeService.scheduleOnce(startTime)(task.run())

  override private[time] def setTaiOffset(offset: Int): Unit                   = timeService.setTaiOffset(offset)
  override def toTai(utcInstant: CswInstant.UtcInstant): CswInstant.TaiInstant = timeService.toTai(utcInstant)
  override def toUtc(taiInstant: CswInstant.TaiInstant): CswInstant.UtcInstant = timeService.toUtc(taiInstant)
}
