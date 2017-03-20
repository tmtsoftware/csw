package csw.services.location.scaladsl

import csw.services.location.internal._
import csw.services.location.internal.wrappers.{JmDnsApiFactory, JmDnsReal}
import csw.services.location.models.Location

object LocationServiceFactory {
  def make(actorRuntime: ActorRuntime): LocationService = make(actorRuntime, JmDnsReal)

  def make(actorRuntime: ActorRuntime, jmDnsApiFactory: JmDnsApiFactory): LocationService = {
    import actorRuntime._
    val (queue, locationStream) = StreamExt.coupling[Location]
    val jmDnsApi = jmDnsApiFactory.make(actorRuntime, queue)
    val locationEventStream = new LocationEventStream(locationStream, jmDnsApi, actorRuntime)
    new LocationServiceImpl(jmDnsApi, actorRuntime, locationEventStream)
  }
}
