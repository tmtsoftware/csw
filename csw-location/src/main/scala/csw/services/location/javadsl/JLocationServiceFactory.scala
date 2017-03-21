package csw.services.location.javadsl

import csw.services.location.internal.JLocationServiceImpl
import csw.services.location.internal.wrappers.{JmDnsApiFactory, JmDnsReal}
import csw.services.location.scaladsl.{ActorRuntime, LocationService, LocationServiceFactory}

object JLocationServiceFactory {
  def make(actorRuntime: ActorRuntime): ILocationService = make(actorRuntime, JmDnsReal)

  def make(actorRuntime: ActorRuntime, jmDnsApiFactory: JmDnsApiFactory): ILocationService = {
    val locationService: LocationService = LocationServiceFactory.make(actorRuntime, jmDnsApiFactory)
    new JLocationServiceImpl(locationService, actorRuntime)
  }
}
