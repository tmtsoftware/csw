package csw.time.core.util
import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit.NANOSECONDS

import csw.time.core.models.{TAITime, TMTTime, UTCTime}

import scala.concurrent.duration.FiniteDuration

object TMTTimeUtil {

  def delayFrom(time: TMTTime): FiniteDuration = {
    val now      = instantFor(time)
    val duration = Duration.between(now, time.value)
    FiniteDuration(duration.toNanos, NANOSECONDS)
  }

  def instantFor(time: TMTTime): Instant = time match {
    case _: UTCTime ⇒ UTCTime.now().value
    case _: TAITime ⇒ TAITime.now().value
  }
}
