package csw.services.alarm.client.internal.auto_refresh

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import csw.services.alarm.client.internal.AlarmServiceLogger
import csw.services.alarm.client.internal.auto_refresh.AutoRefreshSeverityMessage.{RefreshSeverity, SetSeverityAndAutoRefresh}
import csw.services.logging.scaladsl.Logger

import scala.concurrent.duration.DurationInt

object AutoRefreshSeverityActor {

  def behavior(
      timerScheduler: TimerScheduler[AutoRefreshSeverityMessage],
      alarm: Refreshable
  ): Behavior[AutoRefreshSeverityMessage] = Behaviors.receive { (ctx, msg) ⇒
    val refreshInSeconds: Int = ctx.system.settings.config.getInt("alarm.refresh-in-seconds")
    val log: Logger           = AlarmServiceLogger.getLogger(ctx)

    log.debug(s"AutoRefreshSeverityActor received message :[$msg]")

    msg match {
      case SetSeverityAndAutoRefresh(key, severity) ⇒
        alarm.refreshSeverity(key, severity) // fire and forget the refreshing of severity and straight away start the timer
        timerScheduler.startPeriodicTimer(key.name, RefreshSeverity(key, severity), refreshInSeconds.seconds)
      case RefreshSeverity(key, severity) ⇒ alarm.refreshSeverity(key, severity) //fire and forget the refreshing of severity
    }
    Behaviors.same
  }

}
