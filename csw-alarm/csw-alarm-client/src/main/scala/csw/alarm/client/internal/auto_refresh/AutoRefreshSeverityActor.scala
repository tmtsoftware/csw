package csw.alarm.client.internal.auto_refresh

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import csw.alarm.client.internal.AlarmServiceLogger
import csw.alarm.client.internal.auto_refresh.AutoRefreshSeverityMessage._
import csw.logging.core.scaladsl.Logger

import scala.concurrent.duration.FiniteDuration

object AutoRefreshSeverityActor {

  def behavior(
      timerScheduler: TimerScheduler[AutoRefreshSeverityMessage],
      alarm: Refreshable,
      refreshInterval: FiniteDuration
  ): Behavior[AutoRefreshSeverityMessage] = Behaviors.setup[AutoRefreshSeverityMessage] { ctx ⇒
    val log: Logger = AlarmServiceLogger.getLogger(ctx)

    Behaviors.receiveMessage { msg ⇒
      log.debug(s"AutoRefreshSeverityActor received message :[$msg]")

      msg match {
        case AutoRefreshSeverity(key, severity) ⇒
          alarm.refreshSeverity(key, severity) // fire and forget the refreshing of severity and straight away start the timer
          timerScheduler.startPeriodicTimer(key, SetSeverity(key, severity), refreshInterval)

        case SetSeverity(key, severity) ⇒ alarm.refreshSeverity(key, severity) //fire and forget the refreshing of severity
        case CancelAutoRefresh(key)     ⇒ timerScheduler.cancel(key)
      }
      Behaviors.same
    }
  }
}
