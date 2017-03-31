package csw.services.location.javadsl

import java.util.Optional
import java.util.concurrent.CompletionStage
import java.{util => ju}

import akka.Done
import akka.stream.KillSwitch
import akka.stream.javadsl.Source
import csw.services.location.models._

/**
  * A `LocationService` interface which allows you to manage connections and their registrations.
  * All operations are async, hence yield a `CompletionStage`
  */
trait ILocationService {

  /**
    * Registers the connection information and returns a `CompletionStage` which completes with Registration result which
    * can be used to unregister the location
    *
    * @param registration The [[csw.services.location.models.Location]] from registration will be used to register with
    *                     akka cluster
    */
  def register(registration: Registration): CompletionStage[IRegistrationResult]

  /**
    * Unregisters the connection
    *
    * @param connection An already registered connection
    * @return A `CompletionStage` which completes after un-registration happens successfully and fails otherwise
    */
  def unregister(connection: Connection): CompletionStage[Done]

  /**
    * Unregisters all connections registered with `LocationService`
    *
    * @return A `CompletionStage` which completes after all locations are unregistered successfully or fails otherwise
    */
  def unregisterAll(): CompletionStage[Done]

  /**
    * Resolves the location based on connection
    *
    * @param connection A connection to resolve to with its registered location
    * @return A `CompletionStage` of `Optional` which completes with the resolved location if found or `Empty` otherwise.
    */
  def resolve(connection: Connection): CompletionStage[Optional[Location]]

  /**
    * Lists all locations registered with `LocationService`
    *
    * @return A `CompletionStage` which completes with a `List` of all registered locations
    */
  def list: CompletionStage[ju.List[Location]]

  /**
    * Filters all locations registered with `LocationService` based on a component type
    *
    * @param componentType This component type will be used to match Locations
    * @return A `CompletionStage` which completes with filtered locations
    */
  def list(componentType: ComponentType): CompletionStage[ju.List[Location]]

  /**
    * Filters all locations registered with `LocationService` based on a hostname
    *
    * @param hostname This hostname will be used to match Locations
    * @return A `Future` which completes with filtered locations
    */
  def list(hostname: String): CompletionStage[ju.List[Location]]

  /**
    * Filters all locations registered with `LocationService` based on a connection type
    *
    * @param connectionType This connection type will be used to match Locations
    * @return A `CompletionStage` which completes with filtered locations
    */
  def list(connectionType: ConnectionType): CompletionStage[ju.List[Location]]

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
    * @return A `CompletionStage` which completes when the location service shuts down
    */
  def shutdown(): CompletionStage[Done]
}
