package csw.services.location.javadsl

import acyclic.skipped
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.{util ⇒ ju}

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.stream.KillSwitch
import akka.stream.javadsl.Source
import csw.messages.location._
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

import scala.concurrent.duration.FiniteDuration

/**
 * A LocationService interface to manage connections and their registrations. All operations are non-blocking.
 */
trait ILocationService {

  /**
   * Registers a connection to location
   *
   * @param registration The Registration holding connection and it's corresponding location to register with `LocationService`
   * @return A CompletableFuture which completes with Registration result
   */
  def register(registration: Registration): CompletableFuture[IRegistrationResult]

  /**
   * Unregisters the connection
   *
   * @param connection An already registered connection
   * @return A CompletableFuture which completes after un-registration happens successfully and fails otherwise
   */
  def unregister(connection: Connection): CompletableFuture[Done]

  /**
   * Unregisters all connections registered
   *
   * @note It is highly recommended to use this method for testing purpose only
   * @return A CompletableFuture which completes after all connections are unregistered successfully or fails otherwise
   */
  def unregisterAll(): CompletableFuture[Done]

  /**
   * Resolve the location for a connection from the local cache
   *
   * @param connection A connection to resolve to with its registered location
   * @return A CompletableFuture which completes with the resolved location if found or Empty otherwise.
   */
  def find[L <: Location](connection: TypedConnection[L]): CompletableFuture[Optional[L]]

  /**
   * Resolves the location based on the given connection
   *
   * @param connection A connection to resolve to with its registered location
   * @param within The time for which a connection is looked-up within `LocationService`
   * @tparam L The concrete Location type returned once the connection is resolved
   * @return A CompletableFuture which completes with the resolved location if found or None otherwise.
   */
  def resolve[L <: Location](connection: TypedConnection[L], within: FiniteDuration): CompletableFuture[Optional[L]]

  /**
   * Lists all locations registered
   *
   * @return A CompletableFuture which completes with a List of all registered locations
   */
  def list: CompletableFuture[ju.List[Location]]

  /**
   * Filters all locations registered based on a component type
   *
   * @param componentType List components of this `componentType`
   * @return A CompletableFuture which completes with filtered locations
   */
  def list(componentType: ComponentType): CompletableFuture[ju.List[Location]]

  /**
   * Filters all locations registered based on a hostname
   *
   * @param hostname List components running on this `hostname`
   * @return A CompletableFuture which completes with filtered locations
   */
  def list(hostname: String): CompletableFuture[ju.List[Location]]

  /**
   * Filters all locations registered based on a connection type
   *
   * @param connectionType List components of this `connectionType`
   * @return A CompletableFuture which completes with filtered locations
   */
  def list(connectionType: ConnectionType): CompletableFuture[ju.List[Location]]

  /**
   * Filters all locations registered based on a prefix.
   *
   * @note All locations having subsystem prefix that starts with the given prefix
   *       value will be listed.
   * @param prefix List components by this `prefix`
   * @return A Future which completes with filtered locations
   */
  def listByPrefix(prefix: String): CompletableFuture[ju.List[AkkaLocation]]

  /**
   * Tracks the connection and send events for modification or removal of its location
   *
   * @param connection The `connection` that is to be tracked
   * @return A stream that emits events related to the connection. It can be cancelled using KillSwitch. This will stop giving
   *         events for earlier tracked connection
   */
  def track(connection: Connection): Source[TrackingEvent, KillSwitch]

  /**
   * Subscribe to tracking events for a connection by providing a consumer
   * For each event accept method of consumer interface is invoked.
   * Use this method if you do not want to handle materialization and happy with a side-effecting callback instead
   *
   * @param connection The `connection` that is to be tracked
   * @param consumer The `Consumer` function that consumes `TrakingEvent`
   * @return A killswitch which can be shutdown to unsubscribe the consumer.
   */
  def subscribe(connection: Connection, consumer: Consumer[TrackingEvent]): KillSwitch

  /**
   * Shuts down the LocationService
   *
   * @see Terminate method in [[csw.services.location.commons.CswCluster]]
   * @note It is recommended not to perform any operation on LocationService after calling this method
   * @param reason The reason explaining the shutdown
   * @return A CompletableFuture which completes when the location service has shutdown successfully
   */
  def shutdown(reason: Reason): CompletableFuture[Done]

  /**
   * Returns the Scala API for this instance of location service
   */
  def asScala: LocationService
}
