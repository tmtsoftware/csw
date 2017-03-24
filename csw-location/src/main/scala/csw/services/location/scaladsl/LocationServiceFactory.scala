package csw.services.location.scaladsl

import csw.services.location.internal._

object LocationServiceFactory {
  def make(actorRuntime: ActorRuntime): LocationService = {
    import actorRuntime._
    val locationServiceImpl = new LocationServiceImpl(actorRuntime)
    actorSystem.actorOf(DeathwatchActor.props(locationServiceImpl))
    locationServiceImpl
  }
}
