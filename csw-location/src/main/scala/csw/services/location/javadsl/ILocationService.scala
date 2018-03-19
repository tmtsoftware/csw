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
   * @param registration the Registration holding connection and it's corresponding location to register with `LocationService`
   * @return a CompletableFuture which completes with Registration result or can fail with
   *         [[csw.services.location.exceptions.RegistrationFailed]] or [[csw.services.location.exceptions.OtherLocationIsRegistered]]
   */
  def register(registration: Registration): CompletableFuture[IRegistrationResult]

  /**
   * Unregisters the connection
   *
   * @param connection an already registered connection
   * @return a CompletableFuture which completes after un-registration happens successfully and fails otherwise with
   *         [[csw.services.location.exceptions.UnregistrationFailed]]
   */
  def unregister(connection: Connection): CompletableFuture[Done]

  /**
   * Unregisters all connections registered
   *
   * @note it is highly recommended to use this method for testing purpose only
   * @return a CompletableFuture which completes after all connections are unregistered successfully or fails otherwise
   *         with [[csw.services.location.exceptions.RegistrationListingFailed]]
   */
  def unregisterAll(): CompletableFuture[Done]

  /**
   * Resolve the location for a connection from the local cache
   *
   * @param connection a connection to resolve to with its registered location
   * @return a CompletableFuture which completes with the resolved location if found or Empty otherwise. It can fail with
   *         [[csw.services.location.exceptions.RegistrationListingFailed]].
   */
  def find[L <: Location](connection: TypedConnection[L]): CompletableFuture[Optional[L]]

  /**
   * Resolves the location based on the given connection
   *
   * @param connection a connection to resolve to with its registered location
   * @param within the time for which a connection is looked-up within `LocationService`
   * @tparam L the concrete Location type returned once the connection is resolved
   * @return a CompletableFuture which completes with the resolved location if found or None otherwise. It can fail with
   *         [[csw.services.location.exceptions.RegistrationListingFailed]].
   */
  def resolve[L <: Location](connection: TypedConnection[L], within: FiniteDuration): CompletableFuture[Optional[L]]

  /**
   * Lists all locations registered
   *
   * @return a CompletableFuture which completes with a List of all registered locations or can fail with
   *         [[csw.services.location.exceptions.RegistrationListingFailed]]
   */
  def list: CompletableFuture[ju.List[Location]]

  /**
   * Filters all locations registered based on a component type
   *
   * @param componentType list components of this `componentType`
   * @return a CompletableFuture which completes with filtered locations or can fail with
   *         [[csw.services.location.exceptions.RegistrationListingFailed]]
   */
  def list(componentType: ComponentType): CompletableFuture[ju.List[Location]]

  /**
   * Filters all locations registered based on a hostname
   *
   * @param hostname list components running on this `hostname`
   * @return a CompletableFuture which completes with filtered locations or can fail with
   *         [[csw.services.location.exceptions.RegistrationListingFailed]]
   */
  def list(hostname: String): CompletableFuture[ju.List[Location]]

  /**
   * Filters all locations registered based on a connection type
   *
   * @param connectionType list components of this `connectionType`
   * @return a CompletableFuture which completes with filtered locations or can fail with
   *         [[csw.services.location.exceptions.RegistrationListingFailed]]
   */
  def list(connectionType: ConnectionType): CompletableFuture[ju.List[Location]]

  /**
   * Filters all locations registered based on a prefix.
   *
   * @note all locations having subsystem prefix that starts with the given prefix
   *       value will be listed
   * @param prefix list components by this `prefix`
   * @return a CompletableFuture which completes with filtered locations or can fail with
   *         [[csw.services.location.exceptions.RegistrationListingFailed]]
   */
  def listByPrefix(prefix: String): CompletableFuture[ju.List[AkkaLocation]]

  /**
   * Tracks the connection and send events for modification or removal of its location
   *
   * @param connection the `connection` that is to be tracked
   * @return A stream that emits events related to the connection. It can be cancelled using KillSwitch. This will stop giving
   *         events for earlier tracked connection
   */
  def track(connection: Connection): Source[TrackingEvent, KillSwitch]

  /**
   * Subscribe to tracking events for a connection by providing a consumer
   * For each event accept method of consumer interface is invoked.
   * Use this method if you do not want to handle materialization and happy with a side-effecting callback instead
   *
   * @param connection the `connection` that is to be tracked
   * @param consumer the `Consumer` function that consumes `TrakingEvent`
   * @return a killswitch which can be shutdown to unsubscribe the consumer
   */
  def subscribe(connection: Connection, consumer: Consumer[TrackingEvent]): KillSwitch

  /**
   * Shuts down the LocationService
   *
   * @see terminate method in [[csw.services.location.commons.CswCluster]]
   * @note it is recommended not to perform any operation on LocationService after calling this method
   * @param reason the reason explaining the shutdown
   * @return a CompletableFuture which completes when the location service has shutdown successfully
   */
  def shutdown(reason: Reason): CompletableFuture[Done]

  /**
   * Returns the Scala API for this instance of location service
   */
  def asScala: LocationService
}
