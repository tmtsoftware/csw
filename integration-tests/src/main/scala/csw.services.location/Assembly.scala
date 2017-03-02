package csw.services.location.integration

import com.typesafe.config.ConfigFactory
import csw.services.location.common.ActorRuntime
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{ComponentId, ComponentType}
import csw.services.location.scaladsl.LocationService

object AssemblyApp {
  private var locationService:LocationService = _
  private val actorRuntime = new ActorRuntime("AssemblySystem")

  def start : LocationService = {
 val componentId = ComponentId("hcd1", ComponentType.HCD)
    val connection = AkkaConnection(componentId)
    locationService = LocationService.make(actorRuntime)
    locationService
  }

  def listLocations = locationService.list.await
}