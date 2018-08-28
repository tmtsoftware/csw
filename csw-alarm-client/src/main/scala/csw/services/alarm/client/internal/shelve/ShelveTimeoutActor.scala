package csw.services.alarm.client.internal.shelve

import java.time.Clock

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import csw.services.alarm.client.internal.AlarmServiceLogger
import csw.services.alarm.client.internal.extensions.TimeExtensions.RichClock
import csw.services.alarm.client.internal.shelve.ShelveTimeoutMessage._
import csw.services.logging.scaladsl.Logger

import scala.compat.java8.DurationConverters.DurationOps

object ShelveTimeoutActor {
  def behavior(
      timerScheduler: TimerScheduler[ShelveTimeoutMessage],
      alarm: Unshelvable,
      shelveTimeout: String
  ): Behavior[ShelveTimeoutMessage] = Behaviors.setup { ctx ⇒
    val log: Logger = AlarmServiceLogger.getLogger(ctx)

    // Use the UTC timezone for the time-being. Once the time service is in place, it can query time service.
    val clock = Clock.systemUTC()

    Behaviors.receiveMessage { msg ⇒
      log.debug(s"ShelveTimeoutActor received message :[$msg]")

      msg match {
        case ScheduleShelveTimeout(key) ⇒
          val duration = clock.untilNext(shelveTimeout).toScala
          timerScheduler.startSingleTimer(key.value, ShelveHasTimedOut(key), duration)
        case CancelShelveTimeout(key) ⇒ timerScheduler.cancel(key.value)
        case ShelveHasTimedOut(key)   ⇒ alarm.unshelve(key)
      }
      Behaviors.same
    }
  }
}
