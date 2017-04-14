package csw.services.location.scaladsl

import akka.Done
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw.services.location.javadsl.ILocationService
import csw.services.location.models._

import scala.concurrent.Future

/**
  * A `LocationService` interface which allows you to manage connections and their registrations.
  * All operations are async, hence yield a [[scala.concurrent.Future]]
  */
trait LocationService {

  /**
    * Registers the connection information.
    *
    * @param registration The [[csw.services.location.models.Location]] from registration will be used to register with
    *                     akka cluster
    * @return A `Future` which completes with Registration result which can be used to unregister the location
    */
  def register(registration: Registration): Future[RegistrationResult]

  /**
    * Unregisters the connection
    *
    * @param connection An already registered connection
    * @return A `Future` which completes after un-registration happens successfully and fails otherwise
    */
  def unregister(connection: Connection): Future[Done]

  /**
    * Unregisters all connections registered with `LocationService`
    *
    * @return A `Future` which completes after all locations are unregistered successfully or fails otherwise
    */
  def unregisterAll(): Future[Done]

  /**
    * Resolves the location based on connection
    *
    * @param connection A connection to resolve to with its registered location
    * @return A `Future` of `Option` which completes with the resolved location if found or `None` otherwise.
    */
  def resolve(connection: Connection): Future[Option[Location]]

  /**
    * Lists all locations registered with `LocationService`
    *
    * @return A `Future` which completes with a `List` of all registered locations
    */
  def list: Future[List[Location]]

  /**
    * Filters all locations registered with `LocationService` based on a component type
    *
    * @param componentType This component type will be used to match Locations
    * @return A `Future` which completes with filtered locations
    */
  def list(componentType: ComponentType): Future[List[Location]]

  /**
    * Filters all locations registered with `LocationService` based on a hostname
    *
    * @param hostname This hostname will be used to match Locations
    * @return A `Future` of `List` which completes with filtered locations
    */
  def list(hostname: String): Future[List[Location]]

  /**
    * Filters all locations registered with `LocationService` based on a connection type
    *
    * @param connectionType This connection type will be used to match Locations
    * @return A `Future` of `List` which completes with filtered locations
    */
  def list(connectionType: ConnectionType): Future[List[Location]]

  /**
    * Tracks the location of a connection
    *
    * @param connection A connection for which it's location will be tracked
    * @return A `Source` that emits events for the connection. Use `KillSwitch` to stop tracking
    *         when no longer needed. This will stop giving events for earlier tracked connection
    */
  def track(connection: Connection): Source[TrackingEvent, KillSwitch]

  /**
    * Shuts down the `LocationService` interface
    *
    * @note It is recommended not to perform any operation on `LocationService` after shutdown
    * @return A `Future` which completes when the location service shuts down
    */
  def shutdown(): Future[Done]

  /**
    * Returns the Java API for this instance of location service
    */
  def asJava: ILocationService
}
