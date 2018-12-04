package csw.aas.core.deployment

import csw.aas.core.deployment.AuthConfig.AuthServiceLocation
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.scaladsl.LocationService

import scala.async.Async.{async, _}
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

/**
 * Resolves the location of server hosting the keycloak service
 *
 * @param locationService an instance of location service that provides api to resolve config server
 */
private[aas] class KeycloakServiceResolver(
    locationService: LocationService,
    keycloakConnection: HttpConnection = HttpConnection(ComponentId("KeycloakServer", ComponentType.Service))
)(implicit executionContext: ExecutionContext) {

  /**
   * Use the resolve api of location service to resolve keycloak server and wait for maximum 5 seconds. If the resolving exceeds
   * 5 seconds, the returning future completes with a RuntimeException that has appropriate message.
   *
   * @return A future that completes with Location representing keycloak server location
   */
  def resolve: Future[AuthServiceLocation] = async {
    val location = await(locationService.resolve(keycloakConnection, 5.seconds)).getOrElse(
      throw new RuntimeException(
        s"config service connection=${keycloakConnection.name} can not be resolved"
      )
    )
    location
  }
}
