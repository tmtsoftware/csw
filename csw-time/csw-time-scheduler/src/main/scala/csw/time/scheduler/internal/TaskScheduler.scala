/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.time.scheduler.internal

import jnr.ffi.*
import TaskScheduler.*
import akka.actor.ActorRef
import csw.time.core.models.{TMTTime, UTCTime}
import csw.time.scheduler.api.{Cancellable, TimeServiceScheduler}

import java.time.Duration
import java.util.concurrent.{CopyOnWriteArrayList, Semaphore}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

object TaskScheduler {
  val CLOCK_MONOTONIC = 1
  val TIMER_ABSTIME   = 1

  private val libraryOptions = new java.util.HashMap[LibraryOption, Any]()
  // load immediately instead of lazily (ie on first use)
  libraryOptions.put(LibraryOption.LoadNow, true)
  private val libName           = Platform.getNativePlatform.getStandardCLibraryName
  private[internal] val libc    = LibraryLoader.loadLibrary(classOf[LibC], libraryOptions, libName)
  private[internal] val runtime = Runtime.getRuntime(libc)

  // Represents the C timespec struct
  private[internal] class Timespec(val runtime: Runtime) extends Struct(runtime) {
    // Struct types can be found as inner classes of the Struct class
    val tv_sec  = new SignedLong()
    val tv_nsec = new SignedLong()

    // You can add your own methods of choice
    def setTime(sec: Long, nsec: Long): Unit = {
      tv_sec.set(sec)
      tv_nsec.set(nsec)
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
    private[internal] val sem = new Semaphore(1)
    sem.acquire()
    private[internal] val tickCount = new AtomicLong(0)
    private[internal] val running   = new AtomicBoolean(true)
    new Thread(this).start()

    /**
     * Stops the task and ends the thread
     */
    def stop(): Unit = running.set(false)

    override def run(): Unit = {
      while (running.get()) {
        sem.acquire()
        if (maybeStartTime.isDefined && UTCTime.now().value.compareTo(maybeStartTime.get.value) >= 0) {
          scan()
          if (once) {
            stop()
          }
        }
      }
    }
    // method executed every waitTicks ticks
    def scan(): Unit
  }
}

class TaskScheduler {
  // list of scheduled tasks
  private val tasks    = new CopyOnWriteArrayList[ScanTask]()
  private val stopFlag = new AtomicBoolean(false)

  def addTask(task: ScanTask): Boolean    = tasks.add(task)
  def removeTask(task: ScanTask): Boolean = tasks.remove(task)

  def start(): Unit = {
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
        task.tickCount.getAndDecrement()
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
