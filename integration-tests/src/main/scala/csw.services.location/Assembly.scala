package csw.services.location.integration

import akka.actor.ActorRef
import csw.services.location.common.ActorRuntime
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.models.Location
import csw.services.location.scaladsl.LocationService

object Assembly {
  private var locationService: LocationService = _
  private val actorRuntime = new ActorRuntime("AssemblySystem")

  def start() : Unit = {
    locationService = LocationService.make(actorRuntime)
  }

  def listLocations: List[Location] = locationService.list.await

  def sendUnregisterMessage(actorRef: ActorRef) : Unit = {
      actorRef ! "Unregister"
  }

}