package csw.services.location.scaladsl

import csw.services.location.internal._

object LocationServiceFactory {
  def make(actorRuntime: ActorRuntime): LocationService = new LocationServiceCrdtImpl(actorRuntime)
}
