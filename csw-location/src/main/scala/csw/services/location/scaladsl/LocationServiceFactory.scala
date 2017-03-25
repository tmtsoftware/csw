package csw.services.location.scaladsl

import akka.actor.ActorSystem
import csw.services.location.internal._

object LocationServiceFactory {
  def make(actorSystem: ActorSystem): LocationService = make(new ActorRuntime(actorSystem))
  def make(actorRuntime: ActorRuntime): LocationService = {
    import actorRuntime._
    val locationServiceImpl = new LocationServiceImpl(actorRuntime)
    actorSystem.actorOf(DeathwatchActor.props(locationServiceImpl))
    locationServiceImpl
  }
}
