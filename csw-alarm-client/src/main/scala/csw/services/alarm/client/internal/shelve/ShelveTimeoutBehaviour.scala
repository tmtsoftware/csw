package csw.services.alarm.client.internal.shelve

import java.time.{ZoneOffset, ZonedDateTime}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, MutableBehavior, TimerScheduler}
import csw.services.alarm.api.models.AlarmKey
import csw.services.alarm.client.internal.extensions.ZonedDateTimeExtensions.{RichInt, RichZonedDateTime}
import csw.services.alarm.client.internal.shelve.ShelveTimeoutMessage.{
  CancelShelveTimeout,
  ScheduleShelveTimeout,
  ShelveHasTimedOut
}

class ShelveTimeoutBehaviour(
    ctx: ActorContext[ShelveTimeoutMessage],
    timerScheduler: TimerScheduler[ShelveTimeoutMessage],
    alarm: UnShelvable
) extends MutableBehavior[ShelveTimeoutMessage] {

  private val shelveTimeoutHour: Int = ctx.system.settings.config.getInt("alarm.shelve-timeout-hour-of-day")

  override def onMessage(msg: ShelveTimeoutMessage): Behavior[ShelveTimeoutMessage] = {
    msg match {
      case ScheduleShelveTimeout(key) ⇒ scheduleShelveTimeout(key)
      case CancelShelveTimeout(key)   ⇒ timerScheduler.cancel(key.name)
      case ShelveHasTimedOut(key)     ⇒ alarm.unShelve(key)
    }
    this
  }

  private def scheduleShelveTimeout(key: AlarmKey): Unit = {
    val currentTime   = ZonedDateTime.now(ZoneOffset.UTC)
    val shelveTimeout = shelveTimeoutHour.toHourOfDay - currentTime
    timerScheduler.startSingleTimer(key.name, ShelveHasTimedOut(key), shelveTimeout)
  }
}
