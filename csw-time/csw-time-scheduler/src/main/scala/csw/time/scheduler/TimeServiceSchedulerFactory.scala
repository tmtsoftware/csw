package csw.time.scheduler

import akka.actor.ActorSystem
import csw.time.scheduler.api.TimeServiceScheduler
import csw.time.scheduler.internal.TimeServiceSchedulerImpl

/**
 * Factory to create [[csw.time.scheduler.api.TimeServiceScheduler]]
 */
object TimeServiceSchedulerFactory {

  /**
   * API to create [[csw.time.scheduler.api.TimeServiceScheduler]]
   *
   * @param actorSystem an actorSystem required for scheduling tasks
   * @return an instance of [[csw.time.scheduler.api.TimeServiceScheduler]] which can be used to schedule one-time/periodic tasks
   */
  def make()(implicit actorSystem: ActorSystem): TimeServiceScheduler = new TimeServiceSchedulerImpl()

}
