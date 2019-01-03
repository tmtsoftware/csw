package csw.time.api
import java.time.Duration

import csw.time.api.models.TMTTime

/**
 * Scheduler for scheduling periodic/non-periodic tasks at a specified time and/or interval.
 * It supports scheduling on both [[csw.time.api.models.UTCTime]] and [[csw.time.api.models.TAITime]].
 * Each API returns a [[Cancellable]] which allows users to cancel the execution of tasks.
 * Please note that implementation of Scheduler is optimised for high-throughput
 * and high-frequency events. It is not to be confused with long-term schedulers such as Quartz.
 */
trait TimeServiceScheduler {

  /**
   * Schedules a task to execute once at the given start time.
   *
   * @param startTime the time at which the task should start its execution
   * @param task the task to be scheduled for execution
   * @return a handle to cancel the execution of the task if it hasn't been executed already
   */
  def scheduleOnce(startTime: TMTTime)(task: ⇒ Unit): Cancellable

  /**
   * Schedules a task to execute once at the given start time.
   *
   * @param startTime the time at which the task should start its execution
   * @param task the task to be scheduled for execution
   * @return a handle to cancel the execution of the task if it hasn't been executed already
   */
  def scheduleOnce(startTime: TMTTime, task: Runnable): Cancellable

  /**
   * Schedules a task to execute periodically at the given interval. The task is executed once immediately without any initial delay.
   * In case you do not want to start scheduling immediately, you can use the overloaded method for [[schedulePeriodically()]] with startTime.
   *
   * @param interval the time interval between the execution of tasks
   * @param task the task to execute at each interval
   * @return a handle to cancel execution of further tasks
   */
  def schedulePeriodically(interval: Duration)(task: ⇒ Unit): Cancellable

  /**
   * Schedules a task to execute periodically at the given interval. The task is executed once immediately without any initial delay.
   * In case you do not want to start scheduling immediately, you can use the overloaded method for [[schedulePeriodically()]] with startTime.
   *
   * @param interval the time interval between the execution of tasks
   * @param task the task to execute at each interval
   * @return a handle to cancel execution of further tasks
   */
  def schedulePeriodically(interval: Duration, task: Runnable): Cancellable

  /**
   * Schedules a task to execute periodically at the given interval. The task is executed once at the given start time followed by execution of task at each interval.
   *
   * @param startTime first time at which task is to be executed
   * @param interval the time interval between the execution of tasks
   * @param task the task to execute after each interval
   * @return a handle to cancel execution of further tasks
   */
  def schedulePeriodically(startTime: TMTTime, interval: Duration)(task: ⇒ Unit): Cancellable

  /**
   * Schedules a task to execute periodically at the given interval. The task is executed once at the given start time followed by execution of task at each interval.
   *
   * @param startTime first time at which task is to be executed
   * @param interval the time interval between the execution of tasks
   * @param task the task to execute after each interval
   * @return a handle to cancel the execution of further tasks
   */
  def schedulePeriodically(startTime: TMTTime, interval: Duration, task: Runnable): Cancellable
}
