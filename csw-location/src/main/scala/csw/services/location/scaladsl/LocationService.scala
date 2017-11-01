package csw.services.location.scaladsl

import acyclic.skipped
import akka.Done
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw.messages.location._
import csw.services.location.javadsl.ILocationService
import csw.services.location.models._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
 * A LocationService interface to manage registrations. All operations are non-blocking.
 */
trait LocationService {

  /**
   * Registers a connection -> location in cluster
   *
   * @return A Future which completes with Registration result
   */
  def register(registration: Registration): Future[RegistrationResult]

  /**
   * Unregisters the connection
   *
   * @param connection An already registered connection
   * @return A Future which completes after un-registration happens successfully and fails otherwise
   */
  def unregister(connection: Connection): Future[Done]

  /**
   * Unregisters all connections
   *
   * @return A Future which completes after all connections are unregistered successfully or fails otherwise
   * @note It is highly recommended to use this method for testing purpose only
   */
  def unregisterAll(): Future[Done]

  /**
   * Resolves the location for a connection from the local cache
   *
   * @param connection A connection to resolve to with its registered location
   * @return A Future which completes with the resolved location if found or None otherwise.
   */
  def find[L <: Location](connection: TypedConnection[L]): Future[Option[L]]

  /**
   * Resolves the location for a connection from the local cache, if not found waits for the event to arrive
   * within specified time limit. Returns None if both fail.
   *
   * @param connection A connection to resolve to with its registered location
   * @param within Max wait time for event to arrive
   * @return A Future which completes with the resolved location if found or None otherwise.
   */
  def resolve[L <: Location](connection: TypedConnection[L], within: FiniteDuration): Future[Option[L]]

  /**
   * Lists all locations registered
   *
   * @return A Future which completes with a List of all registered locations
   */
  def list: Future[List[Location]]

  /**
   * Filters all locations registered based on a component type
   *
   * @return A Future which completes with filtered locations
   */
  def list(componentType: ComponentType): Future[List[Location]]

  /**
   * Filters all locations registered based on a hostname
   *
   * @return A Future which completes with filtered locations
   */
  def list(hostname: String): Future[List[Location]]

  /**
   * Filters all locations registered based on a connection type
   *
   * @return A Future which completes with filtered locations
   */
  def list(connectionType: ConnectionType): Future[List[Location]]

  /**
   * Filters all locations registered based on a prefix. Note that all locations having subsystem prefix that starts with the given prefix
   * value will be listed.
   *
   * @return A Future which completes with filtered locations
   */
  def listByPrefix(prefix: String): Future[List[AkkaLocation]]

  /**
   * Tracks the connection and send events for modification or removal of its location
   *
   * @return A stream that emits events related to the connection. It can be cancelled using KillSwitch. This will stop giving
   *         events for earlier tracked connection
   */
  def track(connection: Connection): Source[TrackingEvent, KillSwitch]

  /**
   * Subscribe to tracking events for a connection by providing a callback
   * For each event the callback is invoked.
   * Returns a killswitch which can be shutdown to unsubscribe the callback.
   * Use this method if you do not want to handle materialization and happy with a side-effecting callback instead
   */
  def subscribe(connection: Connection, callback: TrackingEvent ⇒ Unit): KillSwitch

  /**
   * Shuts down the LocationService
   *
   * @see terminate method in [[csw.services.location.commons.CswCluster]]
   * @note It is recommended not to perform any operation on LocationService after calling this method
   * @return A Future which completes when the location service has shutdown successfully
   */
  def shutdown(): Future[Done]

  /**
   * Returns the Java API for this instance of location service
   */
  def asJava: ILocationService
}
