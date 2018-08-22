package csw.services.alarm.client.internal.shelve

import java.time.{ZoneOffset, ZonedDateTime}

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ManualTime, TestProbe}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.Config
import csw.messages.params.models.Subsystem.NFIRAOS
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.client.internal.extensions.ZonedDateTimeExtensions.{RichInt, RichZonedDateTime}
import csw.services.alarm.client.internal.shelve.ShelveTimeoutMessage.{
  CancelShelveTimeout,
  ScheduleShelveTimeout,
  ShelveHasTimedOut
}
import org.scalatest.FunSuite

import scala.concurrent.duration.DurationDouble

class ShelveTimeoutActorTest extends FunSuite with ActorTestKit {
  override def config: Config      = ManualTime.config
  val manualTime: ManualTime       = ManualTime()
  val tromboneAxisLowLimitAlarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisLowLimitAlarm")

  test("should timeout shelve") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, key ⇒ probe.ref ! "Unshelved", 8))
    )

    actor ! ShelveHasTimedOut(tromboneAxisLowLimitAlarmKey)
    probe.expectMessage("Unshelved")
  }

  test("should schedule shelve timeout") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, key ⇒ probe.ref ! "Unshelved", 8))
    )

    actor ! ScheduleShelveTimeout(tromboneAxisLowLimitAlarmKey)

    manualTime.expectNoMessageFor(3.seconds, probe)
    val duration = 8.toHourOfDay - ZonedDateTime.now(ZoneOffset.UTC)
    manualTime.timePasses(duration)
    probe.expectMessage("Unshelved")
  }

  test("should cancel scheduled shelve timeout") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, key ⇒ probe.ref ! "Unshelved", 8))
    )

    actor ! ScheduleShelveTimeout(tromboneAxisLowLimitAlarmKey)
    actor ! CancelShelveTimeout(tromboneAxisLowLimitAlarmKey)

    val duration = 8.toHourOfDay - ZonedDateTime.now(ZoneOffset.UTC)
    manualTime.timePasses(duration)
    probe.expectNoMessage()
  }
}
