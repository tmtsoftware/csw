package csw.services.alarm.client.internal.shelve

import java.time.{Duration, LocalDate, ZoneOffset, ZonedDateTime}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import csw.services.alarm.client.internal.AlarmServiceLogger
import csw.services.alarm.client.internal.shelve.ShelveTimeoutMessage._
import csw.services.logging.scaladsl.Logger

import scala.compat.java8.DurationConverters.DurationOps
import scala.concurrent.duration.FiniteDuration

object ShelveTimeoutActor {
  def behavior(
      timerScheduler: TimerScheduler[ShelveTimeoutMessage],
      alarm: Unshelvable,
      shelveTimeoutHour: Int
  ): Behavior[ShelveTimeoutMessage] =
    Behaviors.setup { ctx ⇒
      val log: Logger = AlarmServiceLogger.getLogger(ctx)

      Behaviors.receiveMessage { msg ⇒
        log.debug(s"ShelveTimeoutActor received message :[$msg]")

        msg match {
          case ScheduleShelveTimeout(key) ⇒
            timerScheduler.startSingleTimer(key.value, ShelveHasTimedOut(key), durationUntil(shelveTimeoutHour))
          case CancelShelveTimeout(key) ⇒
            timerScheduler.cancel(key.value)
          case ShelveHasTimedOut(key) ⇒
            alarm.unshelve(key)
        }
        Behaviors.same
      }
    }

  def durationUntil(shelveTimeoutHour: Int): FiniteDuration = {
    val currentTime        = ZonedDateTime.now(ZoneOffset.UTC)
    val targetTimeForToday = LocalDate.now().atStartOfDay(ZoneOffset.UTC).withHour(shelveTimeoutHour)
    val durationForToday   = Duration.between(currentTime, targetTimeForToday)
    val duration           = if (durationForToday.isNegative) durationForToday.plusDays(1) else durationForToday
    duration.toScala
  }
}
