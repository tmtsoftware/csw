package csw.services.config.client.internal

import akka.http.scaladsl.model.Uri
import csw.services.location.models.Connection.HttpConnection
import csw.services.location.models.{ComponentId, ComponentType}
import csw.services.location.scaladsl.LocationService

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

/**
 * Resolves the location of server hosting the configuration service
 */
class ConfigServiceResolver(locationService: LocationService, actorRuntime: ActorRuntime) {

  import actorRuntime.ec

  private val configConnection = HttpConnection(ComponentId("ConfigServiceServer", ComponentType.Service))

  def uri: Future[Uri] = async {
    val location = await(locationService.resolve(configConnection, 5.seconds)).getOrElse(
      throw new RuntimeException(s"config service connection=${configConnection.name} can not be resolved")
    )
    Uri(location.uri.toString)
  }
}
