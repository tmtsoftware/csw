package csw.services.location.models

import akka.Done

import scala.concurrent.Future

/**
  * Returned as a result of successful registration.
  */
trait RegistrationResult {
  /**
    * Unregisters the previously registered `Location` and returns a `Future` which completes on successful
    * un-registration
    */
  def unregister(): Future[Done]

  /**
    * A `Location` registered successfully with `LocationService`
    */
  def location: Location
}
