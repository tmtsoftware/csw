package csw.services.location.scaladsl

import csw.services.location.internal._

object LocationServiceFactory {

  def make(): LocationService = make(new ActorRuntime())

  def make(actorRuntime: ActorRuntime): LocationService = {
    val locationService: LocationService = new LocationServiceImpl(actorRuntime)
    DeathwatchActor.startSingleton(actorRuntime, locationService)
    locationService
  }
}
