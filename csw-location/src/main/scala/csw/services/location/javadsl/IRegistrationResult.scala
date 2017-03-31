package csw.services.location.javadsl

import akka.Done
import csw.services.location.models.Location
import java.util.concurrent.CompletionStage

/**
  * Returned as a result of successful registration.
  */
trait IRegistrationResult {

  /**
    * Unregisters the previously registered `Location` and returns a `CompletionStage` which completes on successful
    * un-registration
    */
  def unregister: CompletionStage[Done]

  /**
    * A `Location` registered successfully with `LocationService`
    */
  def location: Location
}
