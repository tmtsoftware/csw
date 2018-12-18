package csw.time.client

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.{ManualTime, ScalaTestWithActorTestKit}
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.testkit.TestProbe
import csw.time.api.TAITime
import csw.time.api.models.Cancellable
import org.scalatest.FunSuiteLike

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationInt

class TimeServiceSchedulerTest extends ScalaTestWithActorTestKit(ManualTime.config) with FunSuiteLike {

  private implicit val untypedSystem: ActorSystem = system.toUntyped
  private val manualTime                          = ManualTime()

  private val timeService = TimeServiceSchedulerFactory.make()

  // DEOPSCSW-542: Schedule a task to execute in future
  test("should schedule task at start time") {
    val testProbe         = TestProbe()
    val probeMsg          = "echo"
    val idealScheduleTime = TAITime(TAITime.now().value.plusSeconds(1))

    val cancellable = timeService.scheduleOnce(idealScheduleTime)(testProbe.ref ! probeMsg)
    manualTime.timePasses(500.millis)
    // check immediately after 5 millis, there should be no message
    testProbe.expectNoMessage(0.millis)
    manualTime.timePasses(500.millis)
    testProbe.expectMsg(probeMsg)
    cancellable.cancel()
  }

  // DEOPSCSW-544: Schedule a task to be executed repeatedly
  // DEOPSCSW-547: Cancel scheduled timers for periodic tasks
  test("should schedule a task periodically at given interval") {
    val buffer: ArrayBuffer[Int] = ArrayBuffer.empty
    val atomicInt                = new AtomicInteger(0)
    val cancellable: Cancellable = timeService.schedulePeriodically(Duration.ofMillis(100)) {
      buffer += atomicInt.getAndIncrement()
    }
    manualTime.timePasses(500.millis)
    cancellable.cancel()
    buffer shouldBe ArrayBuffer(0, 1, 2, 3, 4, 5)
  }

  // DEOPSCSW-544: Start a repeating task with initial offset
  // DEOPSCSW-547: Cancel scheduled timers for periodic tasks
  test("should schedule a task periodically at given interval after start time") {
    val buffer: ArrayBuffer[Int] = ArrayBuffer.empty
    val atomicInt                = new AtomicInteger(0)
    val startTime: TAITime       = new TAITime(TAITime.now().value.plusSeconds(1))

    val cancellable: Cancellable = timeService.schedulePeriodically(startTime, Duration.ofMillis(100)) {
      buffer += atomicInt.getAndIncrement()
    }
    manualTime.timePasses(1.seconds)
    buffer shouldBe ArrayBuffer(0)
    manualTime.timePasses(500.millis)
    cancellable.cancel()
    buffer shouldBe ArrayBuffer(0, 1, 2, 3, 4, 5)
  }
}
