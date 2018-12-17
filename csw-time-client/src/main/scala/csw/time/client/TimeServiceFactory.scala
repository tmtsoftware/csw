package csw.time.client

import akka.actor.ActorSystem
import csw.time.api.TimeService
import csw.time.client.internal.TimeServiceImpl

object TimeServiceFactory {

  def make()(implicit actorSystem: ActorSystem): TimeService = new TimeServiceImpl()

}
