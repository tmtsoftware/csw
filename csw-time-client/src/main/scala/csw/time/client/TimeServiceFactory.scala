package csw.time.client

import akka.actor.ActorSystem
import csw.time.api.scaladsl.TimeService
import csw.time.client.internal.{TMTClock, TimeServiceImpl}

object TimeServiceFactory {

  def make()(implicit actorSystem: ActorSystem): TimeService = new TimeServiceImpl(TMTClock.instance())

  //todo: make this private. Currently its not accessible from java file with package private
  /** for testing */
  def make(offset: Int)(implicit actorSystem: ActorSystem): TimeService = {
    val timeService: TimeService = new TimeServiceImpl(TMTClock.instance(offset))
    timeService.setTaiOffset(offset)
    timeService
  }

}
