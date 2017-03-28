package csw.services.location.scaladsl

import akka.Done
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw.services.location.models._

import scala.concurrent.Future

/**
  * A `LocationService` interface which allows you to manage connections and their registrations.
  * All operations are async, hence yield a [[scala.concurrent.Future]]
  */
trait LocationService {

  /**
    * Asynchronously registers the connection information.
    *
    * @param registration The [[csw.services.location.models.Location]] from registration will be used to register with
    *                     akka cluster
    * @return A `Future` which completes with Registration result which can be used to unregister the location
    */
  def register(registration: Registration): Future[RegistrationResult]

  /**
    * Asynchronously unregisters the connection
    *
    * @param connection An already registered connection
    * @return A `Future` which completes after un-registration happens successfully and fails otherwise
    */
  def unregister(connection: Connection): Future[Done]

  /**
    * Asynchronously unregisters all connections registered via `LocationService`
    *
    * @return A `Future` which completes with `Success` after all locations are unregistered successfully or fails otherwise
    */
  def unregisterAll(): Future[Done]

  /**
    * Asynchronously resolve the location based on connection
    *
    * @param connection A connection to resolve to with its registered location
    * @return A `Future` of `Option` which completes with the resolved location if found or `None` otherwise.
    */
  def resolve(connection: Connection): Future[Option[Location]]

  /**
    * Asynchronously list all locations registered with `LocationService`
    *
    * @return A `Future` of `List` which completes with all locations currently registered
    */
  def list: Future[List[Location]]

  /**
    * Asynchronously filter all locations registered with `LocationService` based on a component type
    *
    * @param componentType A component type against which all locations will be listed
    * @return A `Future` of `List` which completes with filtered locations
    */
  def list(componentType: ComponentType): Future[List[Location]]

  /**
    * Asynchronously filter all locations registered with `LocationService` based on a hostname
    *
    * @param hostname A hostname against which all locations will be listed
    * @return A `Future` of `List` which completes with filtered locations
    */
  def list(hostname: String): Future[List[Location]]

  /**
    * Asynchronously filter all locations registered with `LocationService` based on a connection type
    *
    * @param connectionType
    * @return A `Future` of `List` which completes with filtered locations
    */
  def list(connectionType: ConnectionType): Future[List[Location]]

  /**
    * Track the location of a connection
    *
    * @param connection A connection for which it's location will be tracked
    * @return A `Source` which continuously gives events for the connection. Use `KillSwitch` to stop tracking
    *         it anytime. This will stop giving events for earlier tracked connection
    */
  def track(connection: Connection): Source[TrackingEvent, KillSwitch]

  /**
    * Shutdown the `LocationService` interface
    *
    * @return A `Future` which completes when the location service shuts down
    */
  def shutdown(): Future[Done]
}
