package csw.services.location.javadsl

import java.util.concurrent.CompletableFuture

import akka.Done
import csw.services.location.models.Location

/**
  * RegistrationResult represents successful registration of a location.
  */
trait IRegistrationResult {

  /**
    * The successful registration of location can be unregistered using this method
    */
  def unregister: CompletableFuture[Done]

  def location: Location
}
