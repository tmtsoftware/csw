package csw.services.location.javadsl

import java.util.concurrent.CompletableFuture

import akka.Done
import csw.messages.location.Location

/**
 * IRegistrationResult represents successful registration of a location
 */
trait IRegistrationResult {

  /**
   * The successful registration of location can be unregistered using this method
   *
   * @note this method is idempotent, which means multiple call to unregister the same connection will be no-op once successfully
   *       unregistered from location service
   * @return a CompletableFuture which completes when the location is is successfully unregistered or fails with
   *         [[csw.services.location.exceptions.UnregistrationFailed]]
   */
  def unregister: CompletableFuture[Done]

  /**
   * The `unregister` method will use the connection of this location to unregister from `LocationService`
   *
   * @return the handle to the `Location` that got registered in `LocationService`
   */
  def location: Location
}
