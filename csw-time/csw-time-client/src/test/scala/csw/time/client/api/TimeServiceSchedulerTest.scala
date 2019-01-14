package csw.time.client.api

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.{ManualTime, ScalaTestWithActorTestKit}
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.testkit.TestProbe
import csw.time.api.models.{TAITime, UTCTime}
import csw.time.client.TimeServiceSchedulerFactory
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
      test(s"[$name] should schedule task atZone start time") {
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
  test("[TAITime] should schedule a task periodically atZone given interval") {
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
  test("[TAITime] should schedule a task periodically atZone given interval after start time") {
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
  test("should schedule multiple tasks atZone same start time") {
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

    val atomicInt    = new AtomicInteger(0)
    val startTime    = UTCTime.now()
    val offset: Long = 100l // milliseconds
    val cancellable: Cancellable = timeService.schedulePeriodically(Duration.ofMillis(offset)) {
      buffer += atomicInt.getAndIncrement()
      testProbe.ref ! UTCTime.now()
    }

    val times = testProbe.receiveN(6, 510.milli).map { case t: UTCTime => t }

    cancellable.cancel()

    // Verify correct number and values
    buffer shouldBe ArrayBuffer(0, 1, 2, 3, 4, 5)

    times.size shouldBe 6
    buffer.foreach { i =>
      times(i).value.toEpochMilli shouldBe (startTime.value.toEpochMilli + offset * i) +- 10
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

    val atomicInt    = new AtomicInteger(0)
    val startTime    = UTCTime(UTCTime.now().value.plusSeconds(1L))
    val offset: Long = 100l // milliseconds
    val cancellable: Cancellable = timeService.schedulePeriodically(startTime, Duration.ofMillis(offset)) {
      buffer += atomicInt.getAndIncrement()
      testProbe.ref ! UTCTime.now()
    }

    val times = testProbe.receiveN(6, 1510.milli).map { case t: UTCTime => t }

    cancellable.cancel()

    // Verify correct number and values
    buffer shouldBe ArrayBuffer(0, 1, 2, 3, 4, 5)

    times.size shouldBe 6
    buffer.foreach { i =>
      times(i).value.toEpochMilli shouldBe (startTime.value.toEpochMilli + offset * i) +- 10
    }
  }

  // DEOPSCSW-544: Schedule a task to execute in future with actual times
  // DEOPSCSW-545: Start a repeating task with an initial offset
  test("repeating task with offset that saves time and checks consistency") {
    // we do not want manual config in this test to compare start time with task execution time
    // hence separate instance of actor system is created here which does not use ManualConfig
    val system      = ActorSystem()
    val timeService = TimeServiceSchedulerFactory.make()(system)
    val testProbe   = TestProbe()(system)
    val testProbe1  = TestProbe()(system)

    // Warm up scheduler call. Not sure this has an effect, but it seems to stabalize initial
    val period = Duration.ofMillis(50)
    timeService.schedulePeriodically(period) {
      testProbe1.ref ! UTCTime.now()
    }
    testProbe1.expectMsgType[UTCTime]

    // Now do the actual test
    val actualStart = UTCTime.now()
    val initialSeconds = 1
    val startTime   = UTCTime(actualStart.value.plusSeconds(initialSeconds))
    val samples     = 100

    // Figure out how long it is necessary to wait for the start time and the samples in millis
    val receiveDelay: Int = initialSeconds + (samples * (period.toMillis) / 1000 + 1).toInt
    //println(s"Start: $actualStart $startTime  -- receiveDelay: $receiveDelay seconds")

    // Run with
    val cancellable: Cancellable = timeService.schedulePeriodically(startTime, period) (testProbe.ref ! UTCTime.now())

    // Wait for the messages, saving times
    val times             = testProbe.receiveN(samples, receiveDelay.seconds).map { case t: UTCTime => t }

    cancellable.cancel()

    // Everything following is tests
    // Check that first is same as start time
    Duration.between(times(0).value, startTime.value).toMillis shouldEqual -0l+-3l

    var last = times(0) // Save the previous value to check consistency of callback
    Range(1, samples).foreach { i =>
      val time  = times(i)
      val timeBetween = Duration.between(last.value, time.value).toMillis
      last = time
      // Check that time between callbacks is reliable
      timeBetween shouldBe period.toMillis +- 3  // ms

      // Allowable error is time between actual time of callback and predicted time of callback
      val allowableError = Duration.ofNanos(4000000l).toNanos
      val diff:Long = Duration.between(startTime.value.plus(i * period.toMillis, ChronoUnit.MILLIS), time.value).toNanos
      diff shouldBe 0l +- allowableError
    }
  }
}
