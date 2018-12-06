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

  private val registrationName = "AAS"
  private val componentId      = ComponentId(registrationName, ComponentType.Service)
  private val httpConnection   = HttpConnection(componentId)

  /**
   * Use the resolve api of location service to resolve keycloak server and wait for maximum 5 seconds. If the resolving exceeds
   * 5 seconds, the returning future completes with a RuntimeException that has appropriate message.
   *
   * @return A future that completes with Location representing keycloak server location
   */
  def resolve(implicit executionContext: ExecutionContext): Future[HttpLocation] = async {
    debug("resolving aas via location service")
    val location = await(locationService.resolve(httpConnection, 5.seconds)).getOrElse(
      {
        error(s"auth service connection=${httpConnection.name} could not be resolved")
        throw new RuntimeException(s"auth service connection=${httpConnection.name} could not be resolved")
      }
    )
    debug(s"aas resolved to ${location.uri.toString}")
    location
  }

  private[csw] def register(): Future[RegistrationResult] = {
    val authServicePort = 8080
    val authServicePath = "auth"
    debug("registering aas with location service")
    val httpRegistration   = HttpRegistration(httpConnection, authServicePort, authServicePath)
    val registrationResult = locationService.register(httpRegistration)
    debug("aas registered with location service")
    registrationResult
  }
}

object AuthServiceLocation {
  def apply(locationService: LocationService): AuthServiceLocation =
    new AuthServiceLocation(locationService)
}
