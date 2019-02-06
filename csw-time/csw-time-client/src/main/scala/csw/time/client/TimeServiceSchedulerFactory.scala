package csw.time.client

import akka.actor.ActorSystem
import csw.time.client.api.TimeServiceScheduler
import csw.time.client.internal.TimeServiceSchedulerImpl

/**
 * Factory to create [[csw.time.client.api.TimeServiceScheduler TimeServiceScheduler]]
 */
object TimeServiceSchedulerFactory {

  /**
   * API to create [[csw.time.client.api.TimeServiceScheduler TimeServiceScheduler]]
   *
   * @param actorSystem an actorSystem required for scheduling tasks
   * @return an instance of [[csw.time.client.api.TimeServiceScheduler TimeServiceScheduler]] which can be used to schedule one-time/periodic tasks
   */
  def make()(implicit actorSystem: ActorSystem): TimeServiceScheduler = new TimeServiceSchedulerImpl()

}
