/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.time.scheduler.internal

import jnr.ffi.*
import TaskScheduler.*

import java.util.concurrent.{CopyOnWriteArrayList, Semaphore}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

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
   * @param name task name
   * @param waitTicks number of ticks between runs (one tick is one millisecond)
   */
  abstract class ScanTask(val name: String, val waitTicks: Int) extends Runnable {
    private[internal] val sem = new Semaphore(1)
    sem.acquire()
    private[internal] val tickCount = new AtomicInteger(0)
    private[internal] val running   = new AtomicBoolean(true)
    new Thread(this).start()

    /**
     * Stops the task and ends the thread
     */
    def stop(): Unit = running.set(false)

    override def run(): Unit = {
      while (running.get()) {
        sem.acquire()
        try {
          scan()
        }
      }
    }
    // method executed every waitTicks ticks
    private[internal] def scan(): Unit
  }
}

class TaskScheduler {
  // list of scheduled tasks
  private val tasks    = new CopyOnWriteArrayList[ScanTask]()
  private val stopFlag = new AtomicBoolean(false)

  def addTask(task: ScanTask): Unit    = tasks.add(task)
  def removeTask(task: ScanTask): Unit = tasks.remove(task)

  def start(): Unit = {
    import TaskScheduler.*
    val deadline = new Timespec(runtime)

    while (!stopFlag.get()) {
      tasks.forEach { task =>
        if (task.tickCount.get() == 0) {
          task.tickCount.set(task.waitTicks)
          task.sem.release()
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

// ------------

//noinspection ScalaStyle
object ScanTaskPerfTestApp extends App {
  val CLOCK_REALTIME = 0

  class MyTask(name: String, intervalMs: Int) extends ScanTask(name, intervalMs) {
    private val intervalNanos   = intervalMs * 1000.0 * 1000.0
    private var count           = 0L
    private val startTime       = new Timespec(runtime)
    private var jitterMicrosecs = 0L
    libc.clock_gettime(CLOCK_REALTIME, startTime)

    override def scan(): Unit = {
      count = count + 1
      val ts = new Timespec(runtime)
      libc.clock_gettime(CLOCK_REALTIME, ts)
      val t1Nanos       = startTime.tv_sec.doubleValue() * 1000.0 * 1000.0 * 1000.0 + startTime.tv_nsec.doubleValue()
      val t2Nanos       = ts.tv_sec.doubleValue() * 1000.0 * 1000.0 * 1000.0 + ts.tv_nsec.doubleValue()
      val diffMicrosecs = Math.abs(((t2Nanos - t1Nanos) - intervalNanos) / 1000).toLong
      if (count > 1000 / intervalMs)
        jitterMicrosecs = (jitterMicrosecs * (count - 1) + diffMicrosecs) / count
      if (count % (1000 / intervalMs) == 0) {
        println(s"$name ($intervalMs ms): jitter = $jitterMicrosecs microsecs (${jitterMicrosecs / 1000.0} ms)")
      }
      libc.clock_gettime(CLOCK_REALTIME, startTime)
    }
  }

  val scheduler = new TaskScheduler()
  scheduler.addTask(new MyTask("1-ms-task-A", 1))
  scheduler.addTask(new MyTask("1-ms-task-B", 1))

  scheduler.addTask(new MyTask("10-ms-task-A", 10))
  scheduler.addTask(new MyTask("10-ms-task-B", 10))

  scheduler.addTask(new MyTask("100-ms-task-A", 100))
  scheduler.addTask(new MyTask("100-ms-task-B", 100))

  scheduler.addTask(new MyTask("1000-ms-task-A", 1000))
  scheduler.addTask(new MyTask("1000-ms-task-B", 1000))

  scheduler.start()
}
