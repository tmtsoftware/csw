package csw.time.client

import akka.actor.ActorSystem
import csw.time.api.TimeServiceScheduler
import csw.time.client.internal.TimeServiceSchedulerImpl

object TimeServiceSchedulerFactory {

  def make()(implicit actorSystem: ActorSystem): TimeServiceScheduler = new TimeServiceSchedulerImpl()

}
