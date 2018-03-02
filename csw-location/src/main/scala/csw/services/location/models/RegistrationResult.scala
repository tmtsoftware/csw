package csw.services.location.models

import akka.Done
import csw.messages.location.Location

import scala.concurrent.Future

/**
 * RegistrationResult represents successful registration of a location
 */
trait RegistrationResult {

  /**
   * The successful registration of location can be unregistered using this method
   */
  def unregister(): Future[Done]

  /**
   * The `unregister` method will use the connection of this location to unregister from `LocationService`
   *
   * @return The handle to the `Location` that got registered in `LocationService`
   */
  def location: Location
}
