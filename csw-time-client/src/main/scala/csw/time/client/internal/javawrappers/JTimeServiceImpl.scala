package csw.time.client.internal.javawrappers
import java.util.function.Consumer

import csw.time.api.javadsl.ITimeService
import csw.time.api.models.{Cancellable, CswInstant}
import csw.time.api.scaladsl.TimeService

import scala.compat.java8.FunctionConverters.enrichAsScalaFromConsumer

class JTimeServiceImpl(timeService: TimeService) extends ITimeService {

  override def utcTime(): CswInstant.UtcInstant = timeService.utcTime()

  override def taiTime(): CswInstant.TaiInstant = timeService.taiTime()

  override def taiOffset(): Int = timeService.taiOffset()

  override def scheduleOnce(
      startTime: CswInstant.TaiInstant,
      task: Consumer[Void]
  ): Cancellable = timeService.scheduleOnce(startTime)(task.asScala)
}
