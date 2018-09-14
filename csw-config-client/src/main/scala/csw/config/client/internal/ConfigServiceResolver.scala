package csw.config.client.internal

import akka.http.scaladsl.model.Uri
import csw.location.api.scaladsl.LocationService
import csw.config.client.commons.ConfigServiceConnection

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

/**
 * Resolves the location of server hosting the configuration service
 *
 * @param locationService an instance of location service that provides api to resolve config server
 * @param actorRuntime ActorRuntime provides the execution context for doing the job of asynchronously resolving config
 *                     server from location service
 */
private[config] class ConfigServiceResolver(locationService: LocationService, actorRuntime: ActorRuntime) {
  import actorRuntime.ec

  /**
   * Use the resolve api of location service to resolve config server and wait for maximum 5 seconds. If the resolving exceeds
   * 5 seconds, the returning future completes with a RuntimeException that has appropriate message.
   *
   * @return A future that completes with URI representing config server location
   */
  def uri: Future[Uri] = async {
    val location = await(locationService.resolve(ConfigServiceConnection.value, 5.seconds)).getOrElse(
      throw new RuntimeException(
        s"config service connection=${ConfigServiceConnection.value.name} can not be resolved"
      )
    )
    Uri(location.uri.toString)
  }
}
