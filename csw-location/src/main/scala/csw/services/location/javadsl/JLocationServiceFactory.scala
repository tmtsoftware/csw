package csw.services.location.javadsl

import csw.services.location.internal.JLocationServiceImpl
import csw.services.location.scaladsl.{ActorRuntime, LocationService, LocationServiceFactory}

object JLocationServiceFactory {

  def make(actorRuntime: ActorRuntime): ILocationService = {
    val locationService: LocationService = LocationServiceFactory.make(actorRuntime)
    new JLocationServiceImpl(locationService, actorRuntime)
  }
}
