package csw.services.alarm.client.internal.shelve

import java.time.{ZoneOffset, ZonedDateTime}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.client.internal.AlarmServiceLogger
import csw.services.alarm.client.internal.extensions.ZonedDateTimeExtensions.{RichInt, RichZonedDateTime}
import csw.services.alarm.client.internal.shelve.ShelveTimeoutMessage.{
  CancelShelveTimeout,
  ScheduleShelveTimeout,
  ShelveHasTimedOut
}
import csw.services.logging.scaladsl.Logger

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
          case ScheduleShelveTimeout(key) ⇒ scheduleShelveTimeout(key, shelveTimeoutHour, timerScheduler)
          case CancelShelveTimeout(key)   ⇒ timerScheduler.cancel(key.value)
          case ShelveHasTimedOut(key)     ⇒ alarm.unshelve(key)
        }
        Behaviors.same
      }
    }

  private def scheduleShelveTimeout(
      key: AlarmKey,
      shelveTimeoutHour: Int,
      timerScheduler: TimerScheduler[ShelveTimeoutMessage]
  ): Unit = {
    val currentTime   = ZonedDateTime.now(ZoneOffset.UTC)
    val shelveTimeout = shelveTimeoutHour.toHourOfDay - currentTime
    timerScheduler.startSingleTimer(key.value, ShelveHasTimedOut(key), shelveTimeout)
  }
}
