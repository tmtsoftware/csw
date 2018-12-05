package csw.time.client

import akka.actor.ActorSystem
import csw.time.api.javadsl.ITimeService
import csw.time.api.scaladsl.TimeService
import csw.time.client.internal.javawrappers.JTimeServiceImpl
import csw.time.client.internal.{TMTClock, TimeServiceImpl}

object TimeServiceFactory {

  def make()(implicit actorSystem: ActorSystem): TimeService = new TimeServiceImpl(TMTClock.instance())

  def jMake(actorSystem: ActorSystem): ITimeService = {
    val timeService = make()(actorSystem)
    new JTimeServiceImpl(timeService)
  }

  //todo: make this private. Currently its not accessible from java file with package private
  /** for testing */
  def jMake(offset: Int, actorSystem: ActorSystem): ITimeService = {
    val timeService = make(offset)(actorSystem)
    new JTimeServiceImpl(timeService)
  }

  /** for testing */
  private[time] def make(offset: Int)(implicit actorSystem: ActorSystem): TimeService = {
    val timeService: TimeService = new TimeServiceImpl(TMTClock.instance(offset))
    timeService.setTaiOffset(offset)
    timeService
  }

}
