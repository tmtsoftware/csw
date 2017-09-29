package csw.services.location.models

import akka.Done
import csw.messages.models.location.Location

import scala.concurrent.Future

/**
 * RegistrationResult represents successful registration of a location.
 */
trait RegistrationResult {

  /**
   * The successful registration of location can be unregistered using this method
   */
  def unregister(): Future[Done]

  def location: Location
}
