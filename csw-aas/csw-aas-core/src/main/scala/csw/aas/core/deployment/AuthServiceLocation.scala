package csw.aas.core.deployment

import csw.aas.core.commons.AuthLogger
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models._
import csw.location.api.scaladsl.LocationService

import scala.async.Async.{async, _}
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

/**
 * Resolves the location of server hosting the keycloak service
 *
 * @param locationService an instance of location service that provides api to resolve config server
 */
class AuthServiceLocation(locationService: LocationService) {

  private val logger = AuthLogger.getLogger
  import logger._

  private val authServicePort  = 8080
  private val registrationName = "Keycloak"
  private val componentId      = ComponentId(registrationName, ComponentType.Service)
  private val httpConnection   = HttpConnection(componentId)

  /**
   * Use the resolve api of location service to resolve keycloak server and wait for maximum 5 seconds. If the resolving exceeds
   * 5 seconds, the returning future completes with a RuntimeException that has appropriate message.
   *
   * @return A future that completes with Location representing keycloak server location
   */
  def resolve(implicit executionContext: ExecutionContext): Future[HttpLocation] = async {
    val location = await(locationService.resolve(httpConnection, 5.seconds)).getOrElse(
      {
        error(s"auth service connection=${httpConnection.name} could not be resolved")
        throw new RuntimeException(
          s"auth service connection=${httpConnection.name} could not be resolved"
        )
      }
    )
    location
  }

  private[csw] def register(): Future[RegistrationResult] = {
    val authServicePath  = "auth"
    val httpRegistration = HttpRegistration(httpConnection, authServicePort, authServicePath)
    locationService.register(httpRegistration)
  }
}

object AuthServiceLocation {
  def apply(locationService: LocationService)(implicit executionContext: ExecutionContext): AuthServiceLocation =
    new AuthServiceLocation(locationService)
}
