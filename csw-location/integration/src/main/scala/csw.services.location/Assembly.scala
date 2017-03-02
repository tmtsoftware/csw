package csw.services.location.integration

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{ComponentId, ComponentType}
import csw.services.location.scaladsl.LocationService

object AssemblyApp {
  var locationService:LocationService = _

  def start : LocationService = {
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=2553").
      withFallback(ConfigFactory.load())
    val actorSystem = ActorSystem("AssemblySystem", config)
    val componentId = ComponentId("hcd1", ComponentType.HCD)
    val connection = AkkaConnection(componentId)
    locationService = LocationService.make(actorSystem)
    locationService
  }

  def listLocations = locationService.list.await
}