package csw.services.alarm.client.internal.auto_refresh

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ManualTime, TestProbe}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.Config
import csw.messages.params.models.Subsystem.NFIRAOS
import csw.services.alarm.api.models.AlarmSeverity.Major
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.client.internal.auto_refresh.AutoRefreshSeverityMessage._
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.collection.mutable
import scala.concurrent.duration.DurationDouble

// DEOPSCSW-491: Auto-refresh an alarm through alarm service cli
class AutoRefreshSeverityActorTest extends FunSuite with ActorTestKit with Eventually with Matchers with BeforeAndAfterAll {

  val tromboneAxisHighLimitAlarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm")
  val tcsAxisHighLimitAlarmKey      = AlarmKey(NFIRAOS, "tcs", "tromboneAxisHighLimitAlarm")
  override def config: Config       = ManualTime.config
  private val manualTime            = ManualTime()

  test("should refresh severity") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[AutoRefreshSeverityMessage](
        t ⇒ AutoRefreshSeverityActor.behavior(t, (_, _) ⇒ probe.ref.tell("severity set"), 5.seconds)
      )
    )
    actor ! SetSeverity(tcsAxisHighLimitAlarmKey, Major)
    probe.expectMessage("severity set")
  }

  test("should set severity and refresh it") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[AutoRefreshSeverityMessage](
        t ⇒ AutoRefreshSeverityActor.behavior(t, (_, _) ⇒ probe.ref.tell("severity refreshed"), 5.seconds)
      )
    )

    actor ! AutoRefreshSeverity(tcsAxisHighLimitAlarmKey, Major)

    probe.expectMessage("severity refreshed")
    manualTime.timePasses(5.seconds)
    probe.expectMessage("severity refreshed")
  }

  test("should cancel the refreshing of alarm severity") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[AutoRefreshSeverityMessage](
        t ⇒ AutoRefreshSeverityActor.behavior(t, (_, _) ⇒ probe.ref.tell("severity refreshed"), 5.seconds)
      )
    )

    actor ! AutoRefreshSeverity(tcsAxisHighLimitAlarmKey, Major)

    probe.expectMessage("severity refreshed")
    manualTime.timePasses(5.seconds)
    probe.expectMessage("severity refreshed")

    actor ! CancelAutoRefresh(tcsAxisHighLimitAlarmKey)
    manualTime.expectNoMessageFor(10.seconds)
  }

  test("should refresh for multiple alarms") {
    val queue: mutable.Queue[AlarmKey] = mutable.Queue.empty[AlarmKey]

    val actor = spawn(
      Behaviors.withTimers[AutoRefreshSeverityMessage](
        t ⇒ AutoRefreshSeverityActor.behavior(t, (key, _) ⇒ queue.enqueue(key), 5.seconds)
      )
    )

    actor ! AutoRefreshSeverity(tcsAxisHighLimitAlarmKey, Major)
    actor ! AutoRefreshSeverity(tromboneAxisHighLimitAlarmKey, Major)

    eventually(queue shouldEqual mutable.Queue(tcsAxisHighLimitAlarmKey, tromboneAxisHighLimitAlarmKey))

    actor ! CancelAutoRefresh(tcsAxisHighLimitAlarmKey)

    manualTime.timePasses(5.seconds)

    eventually(
      queue shouldEqual mutable.Queue(tcsAxisHighLimitAlarmKey, tromboneAxisHighLimitAlarmKey, tromboneAxisHighLimitAlarmKey)
    )
  }

}
