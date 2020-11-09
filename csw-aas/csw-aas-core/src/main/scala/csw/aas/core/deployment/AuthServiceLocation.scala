package csw.aas.core.deployment

import csw.aas.core.commons.AASConnection
import csw.location.api.models.{HttpLocation, HttpRegistration, NetworkType}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}

import scala.async.Async.{async, _}
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Resolves the location of server hosting the oAuth service
 *
 * @param locationService an instance of location service that provides api to resolve config server
 */
private[csw] class AuthServiceLocation(locationService: LocationService) {

  /**
   * Use the resolve api of location service to resolve AAS server and wait for provided duration. If the resolving exceeds
   * provided duration, then returning future completes with a RuntimeException that has appropriate message.
   *
   * @return A future that completes with Location representing AAS server location
   */
  def resolve(within: FiniteDuration = 5.seconds)(implicit executionContext: ExecutionContext): Future[HttpLocation] =
    async {
      val location = await(locationService.resolve(AASConnection.value, within)).getOrElse {
        throw AASResolutionFailed(s"auth service connection=${AASConnection.value.name} could not be resolved")
      }
      location
    }

  private[csw] def register(authServicePort: Int): Future[RegistrationResult] = {
    val authServicePath    = "auth"
    val httpRegistration   = HttpRegistration(AASConnection.value, authServicePort, authServicePath, NetworkType.Outside)
    val registrationResult = locationService.register(httpRegistration)
    registrationResult
  }
}

object AuthServiceLocation {
  def apply(locationService: LocationService): AuthServiceLocation = new AuthServiceLocation(locationService)
}
