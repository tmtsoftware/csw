package csw.time.scheduler.internal

import csw.time.scheduler.internal.TaskScheduler.{ScanTask, Timespec, libc, runtime}

//noinspection ScalaStyle
object ScanTaskPerfTestApp extends App {
  val CLOCK_REALTIME = 0

  class MyTask(name: String, intervalMs: Int) extends ScanTask(intervalMs) {
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
