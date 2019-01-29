package csw.alarm.client.internal.auto_refresh

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.client.AutoRefreshSeverityMessage
import csw.alarm.client.AutoRefreshSeverityMessage.{AutoRefreshSeverity, CancelAutoRefresh, SetSeverity}
import csw.alarm.client.internal.AlarmServiceLogger
import csw.logging.api.scaladsl.Logger

import scala.concurrent.duration.FiniteDuration

private[client] object AlarmRefreshActor {

  def behavior(
      timerScheduler: TimerScheduler[AutoRefreshSeverityMessage],
      alarmService: AlarmService,
      refreshInterval: FiniteDuration
  ): Behavior[AutoRefreshSeverityMessage] = Behaviors.setup[AutoRefreshSeverityMessage] { ctx ⇒
    val log: Logger = AlarmServiceLogger.getLogger(ctx)

    Behaviors.receiveMessage { msg ⇒
      log.debug(s"AutoRefreshSeverityActor received message :[$msg]")

      msg match {
        case AutoRefreshSeverity(key, severity) ⇒
          alarmService.setSeverity(key, severity) // fire and forget the refreshing of severity and straight away start the timer
          timerScheduler.startPeriodicTimer(key, SetSeverity(key, severity), refreshInterval)

        case SetSeverity(key, severity) ⇒ alarmService.setSeverity(key, severity) //fire and forget the refreshing of severity
        case CancelAutoRefresh(key)     ⇒ timerScheduler.cancel(key)
      }
      Behaviors.same
    }
  }
}
