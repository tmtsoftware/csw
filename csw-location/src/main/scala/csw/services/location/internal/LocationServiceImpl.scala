package csw.services.location.internal

import akka.Done
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata._
import akka.pattern.ask
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{KillSwitch, OverflowStrategy}
import akka.util.Timeout
import csw.services.location.commons.{CswCluster, LocationServiceLogger}
import csw.services.location.exceptions.{
  OtherLocationIsRegistered,
  RegistrationFailed,
  RegistrationListingFailed,
  UnregistrationFailed
}
import csw.services.location.internal.Registry.AllServices
import csw.services.location.internal.StreamExt.RichSource
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.{DurationDouble, FiniteDuration}

private[location] class LocationServiceImpl(cswCluster: CswCluster)
    extends LocationService
    with LocationServiceLogger.Simple { outer ⇒

  import cswCluster._
  implicit val timeout: Timeout = Timeout(5.seconds)

  /**
   * Register a 'connection -> location' entry in CRDT
    **/
  def register(registration: Registration): Future[RegistrationResult] = async {

    //Get the location from this registration
    val location = registration.location(cswCluster.hostname)
    log.info(s"Registering connection ${registration.connection.name} with location ${location.uri.toString}")

    //Create a message handler for this connection
    val service = new Registry.Service(registration.connection)

    // Registering a location needs to read from other replicas to avoid duplicate location registration before performing the update
    // This approach is inspired from Migration Guide section of https://github.com/patriknw/akka-data-replication
    val initialValue = (replicator ? service.getByMajority).map {
      case x @ GetSuccess(_, _) ⇒ x.get(service.Key)
      case _                    ⇒ service.EmptyValue
    }

    //Create an update message to update the value of connection key. if the current value is None or same as
    //this location then update it with this location. if it is some other location then an exception will be thrown and
    //it will be handled below by ModifyFailure.
    val updateValue = service.update(
      {
        case r @ LWWRegister(Some(`location`) | None) ⇒ r.withValue(Some(location))
        case LWWRegister(Some(otherLocation)) ⇒
          val locationIsRegistered = OtherLocationIsRegistered(location, otherLocation)
          log.error(locationIsRegistered.getMessage, ex = locationIsRegistered)
          throw locationIsRegistered
      },
      await(initialValue)
    )

    //Create a message to update connection -> location map in CRDT
    val updateRegistry = AllServices.update(_ + (registration.connection → location))

    //Send the update message for connection key to replicator. On success, send another message to update connection -> location
    //map. If that is successful then return a registrationResult for this Location. In case of any failure throw an exception.
    val registrationResultF = (replicator ? updateValue).flatMap {
      case _: UpdateSuccess[_] ⇒
        (replicator ? updateRegistry).map {
          case _: UpdateSuccess[_] ⇒ registrationResult(location)
          case _ ⇒
            val registrationFailed = RegistrationFailed(registration.connection)
            log.error(registrationFailed.getMessage, ex = registrationFailed)
            throw registrationFailed
        }
      case ModifyFailure(service.Key, _, cause, _) ⇒
        log.error(cause.getMessage, ex = cause)
        throw cause
      case _ ⇒
        val registrationFailed = RegistrationFailed(registration.connection)
        log.error(registrationFailed.getMessage, ex = registrationFailed)
        throw registrationFailed
    }
    await(registrationResultF)
  }

  /**
   * Unregister the connection from CRDT
   */
  def unregister(connection: Connection): Future[Done] = {
    log.info(s"Un-registering connection ${connection.name}")
    //Create a message handler for this connection
    val service = new Registry.Service(connection)

    //Send an update message to replicator to update the connection key with None. On success send another message to remove the
    //corresponding connection -> location entry from map. In case of any failure throw an exception otherwise return Done.
    (replicator ? service.update(_.withValue(None))).flatMap {
      case x: UpdateSuccess[_] ⇒
        (replicator ? AllServices.update(_ - connection)).map {
          case _: UpdateSuccess[_] ⇒ Done
          case _ ⇒
            val unregistrationFailed = UnregistrationFailed(connection)
            log.error(unregistrationFailed.getMessage, ex = unregistrationFailed)
            throw unregistrationFailed
        }
      case _ ⇒
        val unregistrationFailed = UnregistrationFailed(connection)
        log.error(unregistrationFailed.getMessage, ex = unregistrationFailed)
        throw unregistrationFailed
    }
  }

  /**
   * Unregister all connections from CRDT
   * Note : This method should be used for testing purpose only
   */
  def unregisterAll(): Future[Done] = async {
    log.warn("Un-registering all components from location service")
    //Get all locations registered with CRDT
    val locations = await(list)

    //for each location unregister it's corresponding connection
    await(Future.traverse(locations)(loc ⇒ unregister(loc.connection)))
    Done
  }

  /**
   * Resolves the location for a connection from the local cache
   */
  def find(connection: Connection): Future[Option[Location]] = async {
    log.info(s"Finding location for connection ${connection.name}")
    await(list).find(_.connection == connection)
  }

  /**
   * Resolve a location for the given connection
   */
  override def resolve(connection: Connection, within: FiniteDuration): Future[Option[Location]] = async {
    log.info(s"Resolving location for connection ${connection.name} within ${within.toString()}")
    val foundInLocalCache = await(find(connection))
    if (foundInLocalCache.isDefined) foundInLocalCache else await(resolveWithin(connection, within))
  }

  /**
   * List all locations registered with CRDT
   */
  def list: Future[List[Location]] = (replicator ? AllServices.get).map {
    case x @ GetSuccess(AllServices.Key, _) ⇒ x.get(AllServices.Key).entries.values.toList
    case NotFound(AllServices.Key, _)       ⇒ List.empty
    case _ ⇒
      val listingFailed = RegistrationListingFailed
      log.error(listingFailed.getMessage, ex = listingFailed)
      throw listingFailed
  }

  /**
   * List all locations registered for the given componentType
   */
  def list(componentType: ComponentType): Future[List[Location]] = async {
    await(list).filter(_.connection.componentId.componentType == componentType)
  }

  /**
   * List all locations registered with the given hostname
   */
  def list(hostname: String): Future[List[Location]] = async {
    await(list).filter(_.uri.getHost == hostname)
  }

  /**
   * List all locations registered with the given connection type
   */
  def list(connectionType: ConnectionType): Future[List[Location]] = async {
    await(list).filter(_.connection.connectionType == connectionType)
  }

  /**
   * Track the status of given connection
   */
  def track(connection: Connection): Source[TrackingEvent, KillSwitch] = {
    log.debug(s"Tracking connection ${connection.name}")
    //Create a message handler for this connection
    val service = new Registry.Service(connection)
    //Get a stream that emits messages sent to the actor generated after materialization
    val source = Source.actorRef[Any](256, OverflowStrategy.dropHead).mapMaterializedValue {
      //Subscribe materialized actorRef to the changes in connection so that above stream starts emitting messages
      actorRef ⇒
        replicator ! Subscribe(service.Key, actorRef)
    }

    //Collect only the Changed events for this connection and transform it to location events.
    // If the changed event contains a Location, send LocationUpdated event.
    // If not, location must have been removed, send LocationRemoved event.
    val trackingEvents = source.collect {
      case c @ Changed(service.Key) if c.get(service.Key).value.isDefined ⇒
        LocationUpdated(c.get(service.Key).value.get)
      case c @ Changed(service.Key) ⇒ LocationRemoved(connection)
    }
    //Allow stream to be cancellable by giving it a KillSwitch in mat value.
    // Also, deduplicate identical messages in case multiple DeathWatch actors unregisters the same location.
    trackingEvents.cancellable.distinctUntilChanged
  }

  /**
   * Subscribe to events of a connection by providing a callback.
   */
  override def subscribe(connection: Connection, callback: TrackingEvent ⇒ Unit): KillSwitch = {
    log.info(s"Subscribing to connection ${connection.name}")
    track(connection).to(Sink.foreach(callback)).run()
  }

  /**
   * Terminate the ActorSystem and gracefully leave the akka cluster
   * Note: It is recommended not to perform any operation on LocationService after shutdown
   */
  def shutdown(): Future[Done] = cswCluster.shutdown()

  private def registrationResult(loc: Location): RegistrationResult = new RegistrationResult {
    override def location: Location = loc

    override def unregister(): Future[Done] = outer.unregister(location.connection)
  }

  private def resolveWithin(connection: Connection, waitTime: FiniteDuration): Future[Option[Location]] =
    track(connection)
      .collect {
        case LocationUpdated(location) ⇒ location
      }
      .takeWithin(waitTime)
      .runWith(Sink.headOption)
}
