package csw.services.alarm.client.internal.shelve

import java.time.Clock

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ManualTime, TestProbe}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.Config
import csw.messages.params.models.Subsystem.NFIRAOS
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.client.internal.extensions.TimeExtensions.RichClock
import csw.services.alarm.client.internal.shelve.ShelveTimeoutMessage.{CancelShelveTimeout, ScheduleShelveTimeout}
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSuite, Matchers}

import scala.collection.mutable
import scala.compat.java8.DurationConverters.DurationOps
import scala.concurrent.duration.DurationDouble

// DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
class ShelveTimeoutActorTest extends FunSuite with Matchers with ActorTestKit with Eventually {
  override def config: Config           = ManualTime.config
  val manualTime: ManualTime            = ManualTime()
  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 10.millis)

  val tromboneAxisHighLimitAlarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm")
  val tcsAxisHighLimitAlarmKey      = AlarmKey(NFIRAOS, "tcs", "tromboneAxisHighLimitAlarm")

  test("should schedule shelve timeout") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, _ ⇒ probe.ref.tell("Unshelve called"), "8:00 AM"))
    )

    actor ! ScheduleShelveTimeout(tromboneAxisHighLimitAlarmKey)

    manualTime.expectNoMessageFor(3.seconds, probe)
    val duration = Clock.systemUTC().untilNext("8:00 AM").toScala
    manualTime.timePasses(duration)

    probe.expectMessage("Unshelve called")
  }

  test("should schedule shelve timeout for multiple alarms") {
    val unshelvedAlarms = new mutable.HashSet[AlarmKey]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, unshelvedAlarms.add, "8:00 AM"))
    )

    actor ! ScheduleShelveTimeout(tromboneAxisHighLimitAlarmKey)
    actor ! ScheduleShelveTimeout(tcsAxisHighLimitAlarmKey)

    val duration = Clock.systemUTC().untilNext("8:00 AM").toScala
    manualTime.timePasses(duration)

    eventually(unshelvedAlarms shouldEqual Set(tromboneAxisHighLimitAlarmKey, tcsAxisHighLimitAlarmKey))
  }

  test("should cancel scheduled shelve timeout") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, _ ⇒ probe.ref.tell("Unshelve called"), "8:00 AM"))
    )

    actor ! ScheduleShelveTimeout(tromboneAxisHighLimitAlarmKey)
    actor ! CancelShelveTimeout(tromboneAxisHighLimitAlarmKey)

    val duration = Clock.systemUTC().untilNext("8:10 AM").toScala
    manualTime.expectNoMessageFor(duration, probe)
  }

  test("should cancel scheduled shelve timeout for multiple alarms") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, _ ⇒ probe.ref.tell("Unshelve called"), "8:00 AM"))
    )

    actor ! ScheduleShelveTimeout(tromboneAxisHighLimitAlarmKey)
    actor ! ScheduleShelveTimeout(tcsAxisHighLimitAlarmKey)

    actor ! CancelShelveTimeout(tromboneAxisHighLimitAlarmKey)
    actor ! CancelShelveTimeout(tcsAxisHighLimitAlarmKey)

    // pass the time a little more than expected (8 AM)
    val duration = Clock.systemUTC().untilNext("8:10 AM").toScala
    manualTime.expectNoMessageFor(duration, probe)
  }
}
