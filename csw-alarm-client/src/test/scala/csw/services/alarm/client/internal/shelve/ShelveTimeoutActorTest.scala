package csw.services.alarm.client.internal.shelve

import java.time.{ZoneOffset, ZonedDateTime}

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ManualTime, TestProbe}
import akka.actor.typed.ActorRef
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
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSuite, Matchers}

import scala.collection.mutable
import scala.concurrent.duration.DurationDouble

// DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
class ShelveTimeoutActorTest extends FunSuite with Matchers with ActorTestKit with Eventually {
  override def config: Config           = ManualTime.config
  val manualTime: ManualTime            = ManualTime()
  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 10.millis)

  val tromboneAxisHighLimitAlarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm")
  val tcsAxisHighLimitAlarmKey      = AlarmKey(NFIRAOS, "tcs", "tromboneAxisHighLimitAlarm")

  test("should timeout shelve") {
    val probe: TestProbe[AlarmKey] = TestProbe[AlarmKey]()
    val actor: ActorRef[ShelveTimeoutMessage] = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, probe.ref.tell, 8))
    )

    actor ! ShelveHasTimedOut(tromboneAxisHighLimitAlarmKey)
    probe.expectMessage(tromboneAxisHighLimitAlarmKey)
  }

  test("should schedule shelve timeout") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, _ ⇒ probe.ref.tell("Unshelve called"), 8))
    )

    actor ! ScheduleShelveTimeout(tromboneAxisHighLimitAlarmKey)

    manualTime.expectNoMessageFor(3.seconds, probe)
    val duration = 8.toHourOfDay - ZonedDateTime.now(ZoneOffset.UTC)
    manualTime.timePasses(duration)

    probe.expectMessage("Unshelve called")
  }

  test("should schedule shelve timeout for multiple alarms") {
    val unshelvedAlarms = new mutable.HashSet[AlarmKey]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, unshelvedAlarms.add, 8))
    )

    actor ! ScheduleShelveTimeout(tromboneAxisHighLimitAlarmKey)
    actor ! ScheduleShelveTimeout(tcsAxisHighLimitAlarmKey)

    val duration = 8.toHourOfDay - ZonedDateTime.now(ZoneOffset.UTC) + 10.minutes
    manualTime.timePasses(duration)

    eventually(unshelvedAlarms shouldEqual Set(tromboneAxisHighLimitAlarmKey, tcsAxisHighLimitAlarmKey))
  }

  test("should cancel scheduled shelve timeout") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, _ ⇒ probe.ref.tell("Unshelve called"), 8))
    )

    actor ! ScheduleShelveTimeout(tromboneAxisHighLimitAlarmKey)
    actor ! CancelShelveTimeout(tromboneAxisHighLimitAlarmKey)

    val duration = 8.toHourOfDay - ZonedDateTime.now(ZoneOffset.UTC) + 10.minutes
    manualTime.expectNoMessageFor(duration, probe)
  }

  test("should cancel scheduled shelve timeout for multiple alarms") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, _ ⇒ probe.ref.tell("Unshelve called"), 8))
    )

    actor ! ScheduleShelveTimeout(tromboneAxisHighLimitAlarmKey)
    actor ! ScheduleShelveTimeout(tcsAxisHighLimitAlarmKey)

    actor ! CancelShelveTimeout(tromboneAxisHighLimitAlarmKey)
    actor ! CancelShelveTimeout(tcsAxisHighLimitAlarmKey)

    val duration = 8.toHourOfDay - ZonedDateTime.now(ZoneOffset.UTC) + 10.minutes
    manualTime.expectNoMessageFor(duration, probe)
  }
}
