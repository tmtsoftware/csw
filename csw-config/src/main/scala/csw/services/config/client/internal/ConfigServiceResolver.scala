package csw.services.config.client.internal

import akka.http.scaladsl.model.Uri
import csw.services.location.models.Connection.HttpConnection
import csw.services.location.models.{ComponentId, ComponentType}
import csw.services.location.scaladsl.LocationService

import scala.async.Async._
import scala.concurrent.Future

class ConfigServiceResolver(locationService: LocationService, actorRuntime: ActorRuntime) {

  import actorRuntime.ec

  private val configConnection = HttpConnection(ComponentId("ConfigServiceServer", ComponentType.Service))

  def uri: Future[Uri] = async {
    val location = await(locationService.resolve(configConnection)).getOrElse(
      //TODO: Make a domain specific exception
      throw new RuntimeException(s"config service connection=${configConnection.name} can not be resolved")
    )
    Uri(location.uri.toString)
  }
}
