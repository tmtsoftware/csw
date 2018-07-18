package csw.services.alarm.client.internal.auto_refresh

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, MutableBehavior, TimerScheduler}
import csw.services.alarm.api.models.{AlarmKey, AlarmSeverity}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class AutoRefreshSeverityBehavior(
    ctx: ActorContext[AutoRefreshSeverityMessage],
    timerScheduler: TimerScheduler[AutoRefreshSeverityMessage],
    setSeverity: (AlarmKey, AlarmSeverity) ⇒ Future[Unit]
) extends MutableBehavior[AutoRefreshSeverityMessage] {

  private val refreshInSeconds: Int = ctx.system.settings.config.getInt("alarm.refresh-in-seconds")

  override def onMessage(msg: AutoRefreshSeverityMessage): Behavior[AutoRefreshSeverityMessage] = {
    msg match {
      case SetSeverityAndAutoRefresh(key, severity) ⇒
        setSeverity(key, severity) // fire and forget the setting of severity and straight away start the timer
        timerScheduler.startPeriodicTimer(key.name, SetSeverity(key, severity), refreshInSeconds.seconds)
      case SetSeverity(key, severity) ⇒ setSeverity(key, severity) //fire and forget the setting of severity
    }
    this
  }
}
