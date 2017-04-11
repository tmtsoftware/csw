package csw.services.location.javadsl

import java.util.concurrent.CompletableFuture

import akka.Done
import csw.services.location.models.Location

/**
  * Returned as a result of successful registration.
  */
trait IRegistrationResult {

  /**
    * Unregisters the previously registered `Location` and returns a `CompletableFuture` which completes on successful
    * un-registration
    */
  def unregister: CompletableFuture[Done]

  /**
    * A `Location` registered successfully with `LocationService`
    */
  def location: Location
}
