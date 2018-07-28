package csw.services.alarm.client.internal.auto_refresh

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, MutableBehavior, TimerScheduler}
import csw.services.alarm.client.internal.auto_refresh.AutoRefreshSeverityMessage.{RefreshSeverity, SetSeverityAndAutoRefresh}

import scala.concurrent.duration.DurationInt

class AutoRefreshSeverityBehavior(
    ctx: ActorContext[AutoRefreshSeverityMessage],
    timerScheduler: TimerScheduler[AutoRefreshSeverityMessage],
    alarm: Refreshable
) extends MutableBehavior[AutoRefreshSeverityMessage] {

  private val refreshInSeconds: Int = ctx.system.settings.config.getInt("alarm.refresh-in-seconds")

  override def onMessage(msg: AutoRefreshSeverityMessage): Behavior[AutoRefreshSeverityMessage] = {
    msg match {
      case SetSeverityAndAutoRefresh(key, severity) ⇒
        alarm.refreshSeverity(key, severity) // fire and forget the refreshing of severity and straight away start the timer
        timerScheduler.startPeriodicTimer(key.name, RefreshSeverity(key, severity), refreshInSeconds.seconds)
      case RefreshSeverity(key, severity) ⇒ alarm.refreshSeverity(key, severity) //fire and forget the refreshing of severity
    }
    this
  }
}
