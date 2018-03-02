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
   * @return A CompletableFuture which completes when the location is is successfully unregistered
   */
  def unregister: CompletableFuture[Done]

  /**
   * The `unregister` method will use the connection of this location to unregister from `LocationService`
   *
   * @return The handle to the `Location` that got registered in `LocationService`
   */
  def location: Location
}
