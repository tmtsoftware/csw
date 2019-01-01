package csw.time.client

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.{ManualTime, ScalaTestWithActorTestKit}
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.testkit.TestProbe
import csw.time.api.{TAITime, UTCTime}
import csw.time.api.models.Cancellable
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationInt

class TimeServiceSchedulerTest extends ScalaTestWithActorTestKit(ManualTime.config) with FunSuiteLike with BeforeAndAfterAll {

  private implicit val untypedSystem: ActorSystem = system.toUntyped
  private val manualTime                          = ManualTime()

  private val timeService = TimeServiceSchedulerFactory.make()

  override protected def beforeAll(): Unit = TAITime.setOffset(37)

  // DEOPSCSW-542: Schedule a task to execute in future
  List(
    ("TAITime", () ⇒ TAITime(TAITime.now().value.plusSeconds(1))), // lazily evaluate time when tests are executed
    ("UTCTime", () ⇒ UTCTime(UTCTime.now().value.plusSeconds(1)))
  ).foreach {
    case (name, idealScheduleTime) ⇒
      test(s"[$name] should schedule task at start time") {
        val testProbe = TestProbe()
        val probeMsg  = "echo"

        val cancellable = timeService.scheduleOnce(idealScheduleTime())(testProbe.ref ! probeMsg)
        manualTime.timePasses(500.millis)
        // check immediately after 5 millis, there should be no message
        testProbe.expectNoMessage(0.millis)
        manualTime.timePasses(500.millis)
        testProbe.expectMsg(probeMsg)
        cancellable.cancel()
      }
  }

  // DEOPSCSW-544: Schedule a task to be executed repeatedly
  // DEOPSCSW-547: Cancel scheduled timers for periodic tasks
  test("[TAITime] should schedule a task periodically at given interval") {
    val buffer: ArrayBuffer[Int] = ArrayBuffer.empty
    val atomicInt                = new AtomicInteger(0)
    val cancellable: Cancellable = timeService.schedulePeriodically(Duration.ofMillis(100)) {
      buffer += atomicInt.getAndIncrement()
    }
    manualTime.timePasses(500.millis)
    cancellable.cancel()
    buffer shouldBe ArrayBuffer(0, 1, 2, 3, 4, 5)
  }

  // DEOPSCSW-545: Start a repeating task with initial offset
  // DEOPSCSW-547: Cancel scheduled timers for periodic tasks
  test("[TAITime] should schedule a task periodically at given interval after start time") {
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

  // DEOPSCSW-542: Schedule a task to execute in future
  test("should schedule multiple tasks at same start time") {
    // we do not want manual config in this test to compare start time with task execution time
    // hence separate instance of actor system is created here which does not use ManualConfig
    val system      = ActorSystem()
    val timeService = TimeServiceSchedulerFactory.make()(system)
    val testProbe   = TestProbe()(system)

    val startTime    = UTCTime(UTCTime.now().value.plusSeconds(1))
    val cancellable  = timeService.scheduleOnce(startTime)(testProbe.ref ! UTCTime.now())
    val cancellable2 = timeService.scheduleOnce(startTime)(testProbe.ref ! UTCTime.now())

    val utcTime1 = testProbe.expectMsgType[UTCTime]
    val utcTime2 = testProbe.expectMsgType[UTCTime]

    val expectedTimeSpread = startTime.value.toEpochMilli +- 20
    utcTime1.value.toEpochMilli shouldBe expectedTimeSpread
    utcTime2.value.toEpochMilli shouldBe expectedTimeSpread

    cancellable.cancel()
    cancellable2.cancel()
  }

  // DEOPSCSW-544: Schedule a task to execute in future with actual times
  test("repeating task that also saves time") {
    // we do not want manual config in this test to compare start time with task execution time
    // hence separate instance of actor system is created here which does not use ManualConfig
    val system      = ActorSystem()
    val timeService = TimeServiceSchedulerFactory.make()(system)
    val testProbe   = TestProbe()(system)

    val buffer: ArrayBuffer[Int] = ArrayBuffer.empty

    val atomicInt                = new AtomicInteger(0)
    val startTime                = UTCTime.now()
    val offset:Long              = 100l // milliseconds
    val cancellable: Cancellable = timeService.schedulePeriodically(Duration.ofMillis(offset)) {
      buffer += atomicInt.getAndIncrement()
      testProbe.ref ! UTCTime.now()
    }

    val times = testProbe.receiveN(6, 500.milli).map { case t: UTCTime => t}

    cancellable.cancel()

    // Verify correct number and values
    buffer shouldBe ArrayBuffer(0, 1, 2, 3, 4, 5)

    times.size shouldBe 6
    buffer.foreach { i =>
      times(i).value.toEpochMilli shouldBe (startTime.value.toEpochMilli + offset*i) +- 10
    }
  }

  // DEOPSCSW-544: Schedule a task to execute in future with actual times
  // DEOPSCSW-545: Start a repeating task with an initial offset
  test("repeating task that also saves time but with an offset") {
    // we do not want manual config in this test to compare start time with task execution time
    // hence separate instance of actor system is created here which does not use ManualConfig
    val system      = ActorSystem()
    val timeService = TimeServiceSchedulerFactory.make()(system)
    val testProbe   = TestProbe()(system)

    val buffer: ArrayBuffer[Int] = ArrayBuffer.empty

    val atomicInt                = new AtomicInteger(0)
    val startTime                = UTCTime(UTCTime.now().value.plusSeconds(1L))
    val offset:Long              = 100l // milliseconds
    val cancellable: Cancellable = timeService.schedulePeriodically(startTime, Duration.ofMillis(offset)) {
      buffer += atomicInt.getAndIncrement()
      testProbe.ref ! UTCTime.now()
    }

    val times = testProbe.receiveN(6, 1500.milli).map { case t: UTCTime => t}

    cancellable.cancel()

    // Verify correct number and values
    buffer shouldBe ArrayBuffer(0, 1, 2, 3, 4, 5)

    times.size shouldBe 6
    buffer.foreach { i =>
      times(i).value.toEpochMilli shouldBe (startTime.value.toEpochMilli + offset*i) +- 10
    }
  }
}
