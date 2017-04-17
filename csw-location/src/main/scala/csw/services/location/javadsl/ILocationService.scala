package csw.services.location.javadsl

import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.{util ⇒ ju}

import akka.Done
import akka.stream.KillSwitch
import akka.stream.javadsl.Source
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

/**
  * A `LocationService` interface which allows you to manage connections and their registrations.
  * All operations are async, hence yield a `CompletableFuture`
  */
trait ILocationService {

  /**
    * Registers the connection information and returns a `CompletableFuture` which completes with Registration result which
    * can be used to unregister the location
    *
    * @param registration The [[csw.services.location.models.Location]] from registration will be used to register with
    *                     akka cluster
    */
  def register(registration: Registration): CompletableFuture[IRegistrationResult]

  /**
    * Unregisters the connection
    *
    * @param connection An already registered connection
    * @return A `CompletableFuture` which completes after un-registration happens successfully and fails otherwise
    */
  def unregister(connection: Connection): CompletableFuture[Done]

  /**
    * Unregisters all connections registered with `LocationService`
    *
    * @return A `CompletableFuture` which completes after all locations are unregistered successfully or fails otherwise
    */
  def unregisterAll(): CompletableFuture[Done]

  /**
    * Resolve the location for a connection from the local cache
    *
    * @param connection A connection to resolve to with its registered location
    * @return A `CompletableFuture` of `Optional` which completes with the resolved location if found or `Empty` otherwise.
    */
  def find(connection: Connection): CompletableFuture[Optional[Location]]

  /**
    * Lists all locations registered with `LocationService`
    *
    * @return A `CompletableFuture` which completes with a `List` of all registered locations
    */
  def list: CompletableFuture[ju.List[Location]]

  /**
    * Filters all locations registered with `LocationService` based on a component type
    *
    * @param componentType This component type will be used to match Locations
    * @return A `CompletableFuture` which completes with filtered locations
    */
  def list(componentType: ComponentType): CompletableFuture[ju.List[Location]]

  /**
    * Filters all locations registered with `LocationService` based on a hostname
    *
    * @param hostname This hostname will be used to match Locations
    * @return A `CompletableFuture` which completes with filtered locations
    */
  def list(hostname: String): CompletableFuture[ju.List[Location]]

  /**
    * Filters all locations registered with `LocationService` based on a connection type
    *
    * @param connectionType This connection type will be used to match Locations
    * @return A `CompletableFuture` which completes with filtered locations
    */
  def list(connectionType: ConnectionType): CompletableFuture[ju.List[Location]]

  /**
    * Tracks the location of a connection
    *
    * @param connection A connection to track
    * @return A `Source` that emits events for the connection. Use `KillSwitch` to stop tracking
    *         when no longer needed. This will stop giving events for earlier tracked connection
    */
  def track(connection: Connection): Source[TrackingEvent, KillSwitch]

  /**
    * Shuts down the `LocationService` interface
    *
    * ''Note : '' It is recommended not to perform any operation on `LocationService` after shutdown
    *
    * @return A `CompletableFuture` which completes when the location service shuts down
    */
  def shutdown(): CompletableFuture[Done]

  /**
    * Returns the Scala API for this instance of location service
    */
  def asScala: LocationService
}
