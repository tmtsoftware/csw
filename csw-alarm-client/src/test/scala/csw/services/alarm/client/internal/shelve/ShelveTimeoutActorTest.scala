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
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration.DurationDouble

class ShelveTimeoutActorTest extends FunSuite with Matchers with ActorTestKit {
  override def config: Config       = ManualTime.config
  val manualTime: ManualTime        = ManualTime()
  val tromboneAxisHighLimitAlarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm")
  val tcsAxisHighLimitAlarmKey      = AlarmKey(NFIRAOS, "tcs", "tromboneAxisHighLimitAlarm")

  test("should timeout shelve") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, key ⇒ probe.ref ! "Unshelved", 8))
    )

    actor ! ShelveHasTimedOut(tromboneAxisHighLimitAlarmKey)
    probe.expectMessage("Unshelved")
  }

  test("should schedule shelve timeout") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, key ⇒ probe.ref ! "Unshelved", 8))
    )

    actor ! ScheduleShelveTimeout(tromboneAxisHighLimitAlarmKey)

    manualTime.expectNoMessageFor(3.seconds, probe)
    val duration = 8.toHourOfDay - ZonedDateTime.now(ZoneOffset.UTC)
    manualTime.timePasses(duration)
    probe.expectMessage("Unshelved")
  }

  test("should schedule shelve timeout based on full alarm key") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, key ⇒ probe.ref ! s"Unshelved: ${key.value}", 8))
    )

    actor ! ScheduleShelveTimeout(tromboneAxisHighLimitAlarmKey)
    manualTime.timePasses(1.minute) // delay between scheduling to ensure sequence of expected messages
    actor ! ScheduleShelveTimeout(tcsAxisHighLimitAlarmKey)

    val duration = 8.toHourOfDay - ZonedDateTime.now(ZoneOffset.UTC)
    manualTime.timePasses(duration)

    probe.expectMessage(s"Unshelved: ${tromboneAxisHighLimitAlarmKey.value}")
    probe.expectMessage(s"Unshelved: ${tcsAxisHighLimitAlarmKey.value}")
  }

  test("should cancel scheduled shelve timeout") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, key ⇒ probe.ref ! "Unshelved", 8))
    )

    actor ! ScheduleShelveTimeout(tromboneAxisHighLimitAlarmKey)
    actor ! CancelShelveTimeout(tromboneAxisHighLimitAlarmKey)

    val duration = 8.toHourOfDay - ZonedDateTime.now(ZoneOffset.UTC)
    manualTime.timePasses(duration)
    probe.expectNoMessage()
  }

  test("should cancel scheduled shelve timeout based on full alarm key") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, key ⇒ probe.ref ! s"Unshelved: ${key.value}", 8))
    )

    actor ! ScheduleShelveTimeout(tromboneAxisHighLimitAlarmKey)
    actor ! ScheduleShelveTimeout(tcsAxisHighLimitAlarmKey)

    actor ! CancelShelveTimeout(tromboneAxisHighLimitAlarmKey)
    actor ! CancelShelveTimeout(tcsAxisHighLimitAlarmKey)

    val duration = 8.toHourOfDay - ZonedDateTime.now(ZoneOffset.UTC)
    manualTime.timePasses(duration)

    probe.expectNoMessage()
  }
}
