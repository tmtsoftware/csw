package csw.services.alarm.client.internal.shelve

import java.time.temporal.ChronoField.HOUR_OF_DAY
import java.time.temporal.ChronoUnit
import java.time.{Duration, ZoneOffset, ZonedDateTime}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, MutableBehavior, TimerScheduler}
import csw.services.alarm.api.models.AlarmKey
import csw.services.alarm.client.internal.shelve.ShelveTimeoutMessage.{
  CancelShelveTimeout,
  ScheduleShelveTimeout,
  ShelveHasTimedOut
}

import scala.compat.java8.DurationConverters.DurationOps
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class ShelveTimeoutBehaviour(
    ctx: ActorContext[ShelveTimeoutMessage],
    timerScheduler: TimerScheduler[ShelveTimeoutMessage],
    unShelve: AlarmKey ⇒ Future[Unit]
) extends MutableBehavior[ShelveTimeoutMessage] {

  private val shelveTimeoutInHourOfDay: Int = ctx.system.settings.config.getInt("shelve-timeout-hour-of-day")

  override def onMessage(msg: ShelveTimeoutMessage): Behavior[ShelveTimeoutMessage] = {
    msg match {
      case ScheduleShelveTimeout(key) ⇒ scheduleShelveTimeout(key)
      case CancelShelveTimeout(key)   ⇒ timerScheduler.cancel(key.name)
      case ShelveHasTimedOut(key)     ⇒ unShelve(key)
    }
    this
  }

  private def scheduleShelveTimeout(key: AlarmKey): Unit = {
    val currentTime   = ZonedDateTime.now(ZoneOffset.UTC)
    val shelveTimeout = timeUntilNextHourOfDay(currentTime)
    timerScheduler.startSingleTimer(key.name, ShelveHasTimedOut(key), shelveTimeout)
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
