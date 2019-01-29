package csw.alarm.client.internal.auto_refresh

import akka.Done
import akka.actor.testkit.typed.scaladsl.{ManualTime, ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.alarm.api.models.AlarmSeverity.Major
import csw.alarm.api.models.AutoRefreshSeverityMessage
import csw.alarm.api.models.AutoRefreshSeverityMessage.{AutoRefreshSeverity, CancelAutoRefresh, SetSeverity}
import csw.alarm.api.models.Key.AlarmKey
import csw.params.core.models.Subsystem.NFIRAOS
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

// DEOPSCSW-491: Auto-refresh an alarm through alarm service cli
// DEOPSCSW-507: Auto-refresh utility for component developers
class AlarmRefreshActorTest
    extends ScalaTestWithActorTestKit(ManualTime.config)
    with FunSuiteLike
    with Eventually
    with Matchers
    with BeforeAndAfterAll {

  implicit val actorSystem: ActorSystem[Nothing] = system
  import actorSystem.executionContext

  val tromboneAxisHighLimitAlarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm")
  val tcsAxisHighLimitAlarmKey      = AlarmKey(NFIRAOS, "tcs", "tromboneAxisHighLimitAlarm")
  private val manualTime            = ManualTime()

  private def send[T](msg: T, to: ActorRef[T]): Future[Done] = Future { to ! msg; Done }

  test("should refresh severity") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[AutoRefreshSeverityMessage](
        t ⇒ AlarmRefreshActor.behavior(t, (_, _) ⇒ send("severity set", probe.ref), 5.seconds)
      )
    )
    actor ! SetSeverity(tcsAxisHighLimitAlarmKey, Major)
    probe.expectMessage("severity set")
  }

  test("should set severity and refresh it") {
    val probe = TestProbe[String]()
    val actor = spawn(
      Behaviors.withTimers[AutoRefreshSeverityMessage](
        t ⇒ AlarmRefreshActor.behavior(t, (_, _) ⇒ send("severity refreshed", probe.ref), 5.seconds)
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
        t ⇒ AlarmRefreshActor.behavior(t, (_, _) ⇒ send("severity refreshed", probe.ref), 5.seconds)
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
        t ⇒ AlarmRefreshActor.behavior(t, (key, _) ⇒ Future { queue.enqueue(key); Done }, 5.seconds)
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
