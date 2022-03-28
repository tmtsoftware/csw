/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.time.scheduler.api

import akka.actor.testkit.typed.scaladsl
import akka.actor.testkit.typed.scaladsl.{ManualTime, ScalaTestWithActorTestKit}
import akka.actor.typed
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import akka.testkit.TestProbe
import csw.time.core.models.{TAITime, UTCTime}
import csw.time.scheduler.TimeServiceSchedulerFactory
import org.scalatest.funsuite.AnyFunSuiteLike

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

class TimeServiceSchedulerTest extends ScalaTestWithActorTestKit(ManualTime.config) with AnyFunSuiteLike {

  private val manualTime                    = ManualTime()(system)
  private implicit val scheduler: Scheduler = system.scheduler
  private implicit val ec: ExecutionContext = system.executionContext

  private val timeService = new TimeServiceSchedulerFactory().make()

  // DEOPSCSW-542: Schedule a task to execute in future
  List(
    ("TAITime", () => TAITime(TAITime.now().value.plusSeconds(1))), // lazily evaluate time when tests are executed
    ("UTCTime", () => UTCTime(UTCTime.now().value.plusSeconds(1)))
  ).foreach { case (name, idealScheduleTime) =>
    test(s"[$name] should schedule task at start time | DEOPSCSW-542") {
      val testProbe = TestProbe()(system.toClassic)
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
  // DEOPSCSW-549: Time service api
  test("[TAITime] should schedule a task periodically at given interval | DEOPSCSW-544, DEOPSCSW-547, DEOPSCSW-549") {
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
  // DEOPSCSW-549: Time service api
  test(
    "[TAITime] should schedule a task periodically at given interval after start time | DEOPSCSW-545, DEOPSCSW-547, DEOPSCSW-549"
  ) {
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
  // DEOPSCSW-549: Time service api
  test("should schedule multiple tasks at same start time | DEOPSCSW-542, DEOPSCSW-549") {
    // we do not want manual config in this test to compare start time with task execution time
    // hence separate instance of actor typedSystem is created here which does not use ManualConfig
    val system: ActorSystem[SpawnProtocol.Command] = typed.ActorSystem(SpawnProtocol(), "test1")
    implicit val ec: ExecutionContext              = system.executionContext

    val timeService = new TimeServiceSchedulerFactory()(system.scheduler).make()
    val testProbe   = scaladsl.TestProbe[UTCTime]("blah")(system)

    val startTime    = UTCTime(UTCTime.now().value.plusSeconds(1))
    val cancellable  = timeService.scheduleOnce(startTime)(testProbe.ref ! UTCTime.now())
    val cancellable2 = timeService.scheduleOnce(startTime)(testProbe.ref ! UTCTime.now())

    testProbe.expectMessageType[UTCTime]
    testProbe.expectMessageType[UTCTime]

    cancellable.cancel()
    cancellable2.cancel()

    system.terminate()
    Await.result(system.whenTerminated, 5.seconds)
  }

  // DEOPSCSW-544: Schedule a task to execute in future with actual times
  // DEOPSCSW-549: Time service api
  test("repeating task that also saves time | DEOPSCSW-544, DEOPSCSW-549") {
    // we do not want manual config in this test to compare start time with task execution time
    // hence separate instance of actor typedSystem is created here which does not use ManualConfig
    val system: ActorSystem[SpawnProtocol.Command] = typed.ActorSystem(SpawnProtocol(), "test1")
    implicit val ec: ExecutionContext              = system.executionContext

    val timeService = new TimeServiceSchedulerFactory()(system.scheduler).make()

    val testProbe = scaladsl.TestProbe[UTCTime]()(system)

    val buffer: ArrayBuffer[Int] = ArrayBuffer.empty

    val atomicInt    = new AtomicInteger(0)
    val offset: Long = 500L // milliseconds
    val cancellable: Cancellable = timeService.schedulePeriodically(Duration.ofMillis(offset)) {
      buffer += atomicInt.getAndIncrement()
      testProbe.ref ! UTCTime.now()
    }

    val times = testProbe.receiveMessages(3, 1400.milli).map { t: UTCTime => t }

    cancellable.cancel()

    // Verify correct number and values
    buffer shouldBe ArrayBuffer(0, 1, 2)

    times.size shouldBe 3

    system.terminate()
    Await.result(system.whenTerminated, 5.seconds)
  }

  // DEOPSCSW-544: Schedule a task to execute in future with actual times
  // DEOPSCSW-545: Start a repeating task with an initial offset
  // DEOPSCSW-549: Time service api
  test("repeating task that also saves time but with an offset | DEOPSCSW-544, DEOPSCSW-545, DEOPSCSW-549") {
    // we do not want manual config in this test to compare start time with task execution time
    // hence separate instance of actor typedSystem is created here which does not use ManualConfig
    val system: ActorSystem[SpawnProtocol.Command] = typed.ActorSystem(SpawnProtocol(), "test1")
    implicit val ec: ExecutionContext              = system.executionContext
    val timeService                                = new TimeServiceSchedulerFactory()(system.scheduler).make()
    val testProbe                                  = scaladsl.TestProbe[UTCTime]()(system)

    val buffer: ArrayBuffer[Int] = ArrayBuffer.empty

    val atomicInt    = new AtomicInteger(0)
    val startTime    = UTCTime(UTCTime.now().value.plusSeconds(1L))
    val offset: Long = 500L // milliseconds
    val cancellable: Cancellable = timeService.schedulePeriodically(startTime, Duration.ofMillis(offset)) {
      buffer += atomicInt.getAndIncrement()
      testProbe.ref ! UTCTime.now()
    }

    val times = testProbe.receiveMessages(3, 2400.milli).map { case t: UTCTime => t }

    cancellable.cancel()

    // Verify correct number and values
    buffer shouldBe ArrayBuffer(0, 1, 2)

    times.size shouldBe 3

    system.terminate()
    Await.result(system.whenTerminated, 5.seconds)
  }
}
