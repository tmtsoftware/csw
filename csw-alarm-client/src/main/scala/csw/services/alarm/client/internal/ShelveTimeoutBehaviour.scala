package csw.services.alarm.client.internal

import java.time.temporal.ChronoField.HOUR_OF_DAY
import java.time.temporal.ChronoUnit
import java.time.{Duration, ZoneOffset, ZonedDateTime}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, MutableBehavior, TimerScheduler}
import csw.services.alarm.api.models.AlarmKey
import csw.services.alarm.client.internal.AlarmTimeoutMessage.{CancelShelveTimeout, ScheduleShelveTimeout, ShelveHasTimedOut}

import scala.compat.java8.DurationConverters.DurationOps
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class ShelveTimeoutBehaviour(
    ctx: ActorContext[AlarmTimeoutMessage],
    timerScheduler: TimerScheduler[AlarmTimeoutMessage],
    unShelve: AlarmKey ⇒ Future[Unit]
) extends MutableBehavior[AlarmTimeoutMessage] {

  private val shelveTimeoutInHourOfDay: Int = ctx.system.settings.config.getInt("shelve-timeout-hour-of-day")

  override def onMessage(msg: AlarmTimeoutMessage): Behavior[AlarmTimeoutMessage] = {
    msg match {
      case ScheduleShelveTimeout(key) ⇒
        val currentTime   = ZonedDateTime.now(ZoneOffset.UTC)
        val shelveTimeout = timeUntilNextHourOfDay(currentTime)
        timerScheduler.startSingleTimer(key.name, ShelveHasTimedOut(key), shelveTimeout)
      case CancelShelveTimeout(key) ⇒ timerScheduler.cancel(key.name)
      case ShelveHasTimedOut(key)   ⇒ // ???
    }
    this
  }

  private def timeUntilNextHourOfDay(currentTime: ZonedDateTime): FiniteDuration = {
    val unshelveTime =
      if (currentTime.get(HOUR_OF_DAY) < shelveTimeoutInHourOfDay)
        currentTime
          .withHour(shelveTimeoutInHourOfDay)
          .truncatedTo(ChronoUnit.DAYS)
      else
        currentTime
          .plus(1, ChronoUnit.DAYS)
          .withHour(shelveTimeoutInHourOfDay)
          .truncatedTo(ChronoUnit.DAYS)

    Duration.between(currentTime, unshelveTime).toScala
  }
}
