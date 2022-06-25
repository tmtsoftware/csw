/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.time.scheduler.internal

import jnr.ffi.*
import TaskScheduler.*
import akka.actor.ActorRef
import csw.time.core.models.{TAITime, TMTTime, UTCTime}
import csw.time.scheduler.api.{Cancellable, TimeServiceScheduler}

import java.time.{Duration, Instant}
import java.util.concurrent.{CopyOnWriteArrayList, Semaphore}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

object TaskScheduler {
  val CLOCK_REALTIME  = 0
  val CLOCK_MONOTONIC = 1
  val CLOCK_TAI       = 11

  val TIMER_ABSTIME = 1

  private val libraryOptions = new java.util.HashMap[LibraryOption, Any]()
  // load immediately instead of lazily (ie on first use)
  libraryOptions.put(LibraryOption.LoadNow, true)
  private val libName           = Platform.getNativePlatform.getStandardCLibraryName
  private[internal] val libc    = LibraryLoader.loadLibrary(classOf[LibC], libraryOptions, libName)
  private[internal] val runtime = Runtime.getRuntime(libc)

  // Represents the C timespec struct
  private[internal] class Timespec(val runtime: Runtime) extends Struct(runtime) {
    val tv_sec  = new SignedLong()
    val tv_nsec = new SignedLong()

    def setTime(sec: Long, nsec: Long): Unit = {
      tv_sec.set(sec)
      tv_nsec.set(nsec)
    }

    def incrTime(secIncr: Long, nanoIncr: Long): Unit = {
      val seconds  = tv_sec.longValue() + secIncr
      val nanos = tv_nsec.longValue() + nanoIncr
      val (s, n)  = if (nanos >= 1000000000L) (seconds + 1, nanos - 1000000000) else (seconds, nanos)
      tv_sec.set(s)
      tv_nsec.set(n)
    }
  }

  // noinspection ScalaUnusedSymbol
  // API we use from libc
  private[internal] trait LibC {
    def clock_gettime(clockid: Int, resp: Timespec): Int
    def clock_nanosleep(clockid: Int, flags: Int, request: Timespec, remain: Timespec): Int
  }

  /**
   * Base class of tasks that can be scheduled
   * @param waitTicks number of ticks between runs (one tick is one millisecond)
   */
  abstract class ScanTask(val waitTicks: Long, val once: Boolean = false, val maybeStartTime: Option[TMTTime] = None)
      extends Runnable {
    private[internal] val sem       = new Semaphore(1)
    private[internal] val tickCount = new AtomicLong(0)
    private[internal] val running   = new AtomicBoolean(true)
//    sem.acquire()
    new Thread(this).start()

    /**
     * Stops the task and ends the thread
     */
    def stop(): Unit = running.set(false)

    override def run(): Unit = {
      if (maybeStartTime.isDefined) {
        val startTime = maybeStartTime.get
        val currentTime = startTime match {
          case _: UTCTime => UTCTime.now()
          case _: TAITime => TAITime.now()
        }
        val diff = Duration.between(currentTime.value, startTime.value)
        if (!diff.isZero && !diff.isNegative) {
          val deadline = new Timespec(runtime)
          libc.clock_gettime(CLOCK_MONOTONIC, deadline)
          deadline.incrTime(diff.getSeconds, diff.getNano)
          libc.clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, deadline, null)
        }
      }
      sem.drainPermits()
      while (running.get()) {
        sem.acquire()
        scan()
        if (once) {
          stop()
        }
      }
    }

    // method executed every waitTicks ticks
    def scan(): Unit
  }
}

class TaskScheduler extends Runnable {
  // list of scheduled tasks
  private val tasks    = new CopyOnWriteArrayList[ScanTask]()
  private val stopFlag = new AtomicBoolean(false)
  new Thread(this).start()

  def addTask(task: ScanTask): Boolean    = tasks.add(task)
  def removeTask(task: ScanTask): Boolean = tasks.remove(task)

  def run(): Unit = {
    import TaskScheduler.*
    val deadline = new Timespec(runtime)

    // A tick is one ms
    while (!stopFlag.get()) {
      tasks.forEach { task =>
        if (task.tickCount.get() == 0) {
          task.tickCount.set(task.waitTicks)
          task.sem.release()
          if (!task.running.get()) removeTask(task)
        }
        if (task.tickCount.get() > 0) task.tickCount.getAndDecrement()
      }
      libc.clock_gettime(CLOCK_MONOTONIC, deadline)
      val tv_sec  = deadline.tv_sec.longValue()
      val tv_nsec = deadline.tv_nsec.longValue()
      val d       = tv_nsec + 1000L * 1000L
      val nsec    = d - d % (1000L * 1000L)
      val (s, n)  = if (nsec >= 1000000000L) (tv_sec + 1, nsec - 1000000000) else (tv_sec, nsec)
      deadline.setTime(s, n)
      // noinspection ScalaStyle
      libc.clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, deadline, null)
    }
    // Reset for next run
    stopFlag.set(false)
  }

  /**
   * Stop the scheduler and all tasks
   */
  def stop(): Unit = {
    tasks.forEach { task =>
      task.running.set(false)
    }
    tasks.clear()
    stopFlag.set(true)
  }
}

class TimeServiceSchedulerNative extends TimeServiceScheduler {
  val sched = new TaskScheduler

  private def schedule(task: ScanTask): Cancellable = {
    sched.addTask(task)
    () => {
      task.stop()
      sched.removeTask(task)
    }
  }

  override def scheduleOnce(startTime: TMTTime)(task: => Unit): Cancellable = {
    schedule(new ScanTask(0, once = true, Some(startTime)) {
      override def scan(): Unit = task
    })
  }

  override def scheduleOnce(startTime: TMTTime, task: Runnable): Cancellable = {
    schedule(new ScanTask(0, once = true, Some(startTime)) {
      override def scan(): Unit = task.run()
    })
  }

  override def scheduleOnce(startTime: TMTTime, receiver: ActorRef, message: Any): Cancellable = {
    schedule(new ScanTask(0, once = true, Some(startTime)) {
      override def scan(): Unit = receiver ! message
    })
  }

  override def schedulePeriodically(interval: Duration)(task: => Unit): Cancellable = {
    schedule(new ScanTask(interval.toMillis) {
      override def scan(): Unit = task
    })
  }

  override def schedulePeriodically(interval: Duration, task: Runnable): Cancellable = {
    schedule(new ScanTask(interval.toMillis) {
      override def scan(): Unit = task.run()
    })
  }

  override def schedulePeriodically(interval: Duration, receiver: ActorRef, message: Any): Cancellable = {
    schedule(new ScanTask(interval.toMillis) {
      override def scan(): Unit = receiver ! message
    })
  }

  override def schedulePeriodically(startTime: TMTTime, interval: Duration)(task: => Unit): Cancellable = {
    schedule(new ScanTask(interval.toMillis, maybeStartTime = Some(startTime)) {
      override def scan(): Unit = task
    })
  }

  override def schedulePeriodically(startTime: TMTTime, interval: Duration, task: Runnable): Cancellable = {
    schedule(new ScanTask(interval.toMillis, maybeStartTime = Some(startTime)) {
      override def scan(): Unit = task.run()
    })
  }

  override def schedulePeriodically(startTime: TMTTime, interval: Duration, receiver: ActorRef, message: Any): Cancellable = {
    schedule(new ScanTask(interval.toMillis, maybeStartTime = Some(startTime)) {
      override def scan(): Unit = receiver ! message
    })
  }
}
