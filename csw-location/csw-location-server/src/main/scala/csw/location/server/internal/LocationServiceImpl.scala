package csw.location.server.internal

import akka.Done
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.ddata.Replicator.{ModifyFailure, NotFound, UpdateSuccess}
import akka.cluster.ddata._
import akka.cluster.ddata.typed.scaladsl.Replicator
import akka.cluster.ddata.typed.scaladsl.Replicator.{Changed, GetSuccess}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.{KillSwitch, OverflowStrategy}
import akka.util.Timeout
import csw.location.api.exceptions._
import csw.location.api.scaladsl.LocationServiceE
import csw.location.models._
import csw.location.server.commons.{CswCluster, LocationServiceLogger}
import csw.location.server.internal.Registry.AllServices
import csw.location.server.internal.StreamExt.RichSource
import csw.logging.api.scaladsl.Logger
import msocket.api.models.Subscription

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.{DurationDouble, FiniteDuration}

private[location] class LocationServiceImpl(cswCluster: CswCluster) extends LocationServiceE {
  outer =>

  private val log: Logger = LocationServiceLogger.getLogger

  import cswCluster._
  private implicit val timeout: Timeout = Timeout(5.seconds)

  /**
   * Register a 'connection -> location' entry in CRDT
   */
  def register(registration: Registration): Future[Either[RegistrationError, Location]] = async {

    //Get the location from this registration
    val location = registration.location(cswCluster.hostname)
    log.info(s"Registering connection: [${registration.connection.name}] with location: [${location.uri.toString}]")

    //Create a message handler for this connection
    val service = new Registry.Service(registration.connection)

    // Registering a location needs to read from other replicas to avoid duplicate location registration before performing the update
    // This approach is inspired from Migration Guide section of https://github.com/patriknw/akka-data-replication
    val initialValue = (replicator ? service.getByMajority).map {
      case x @ GetSuccess(_) => x.get(service.Key)
      case _                 => service.EmptyValue
    }

    //Create an update message to update the value of connection key. if the current value is None or same as
    //this location then update it with this location. if it is some other location then an exception will be thrown and
    //it will be handled below by ModifyFailure.
    val updateValue = service.update(
      {
        case r @ LWWRegister(Some(`location`) | None) => r.withValueOf(Some(location))
        case LWWRegister(Some(otherLocation)) =>
          val locationIsRegistered = new OtherLocationIsRegistered(location, otherLocation)
          throw locationIsRegistered
      },
      await(initialValue)
    )

    //Create a message to update connection -> location map in CRDT
    val updateRegistry = AllServices.update(_ :+ (registration.connection -> location))

    //Send the update message for connection key to replicator. On success, send another message to update connection -> location
    //map. If that is successful then return a registrationResult for this Location. In case of any failure throw an exception.
    val registrationResultF: Future[Either[RegistrationError, Location]] = (replicator ? updateValue).flatMap {
      case _: UpdateSuccess[_] =>
        (replicator ? updateRegistry).map {
          case _: UpdateSuccess[_] =>
            log.info(
              s"Successfully registered connection: [${registration.connection.name}] with location [${location.uri}]"
            )
            Right(location)
          case _ =>
            val registrationFailed = new RegistrationFailed(registration.connection)
            log.error(registrationFailed.getMessage, ex = registrationFailed)
            Left(registrationFailed)
        }
      case ModifyFailure(service.Key, _, cause, _) =>
        val otherLocationIsRegistered = new OtherLocationIsRegistered(cause.getMessage)
        log.error(otherLocationIsRegistered.getMessage, ex = otherLocationIsRegistered)
        Future.successful(Left(otherLocationIsRegistered))
      case _ =>
        val registrationFailed = new RegistrationFailed(registration.connection)
        log.error(registrationFailed.getMessage, ex = registrationFailed)
        Future.successful(Left(registrationFailed))
    }
    await(registrationResultF)
  }

  /**
   * Unregister the connection from CRDT
   */
  def unregister(connection: Connection): Future[Either[UnregistrationFailed, Done]] = {
    log.info(s"Un-registering connection: [${connection.name}]")
    //Create a message handler for this connection
    val service = new Registry.Service(connection)

    //Send an update message to replicator to update the connection key with None. On success send another message to remove the
    //corresponding connection -> location entry from map. In case of any failure throw an exception otherwise return Done.
    (replicator ? service.update(_.withValueOf(None))).flatMap {
      case x: UpdateSuccess[_] =>
        (replicator ? AllServices.update(_.remove(node, connection))).map {
          case _: UpdateSuccess[_] => Right(Done)
          case _ =>
            val unregistrationFailed = UnregistrationFailed(connection)
            log.error(unregistrationFailed.getMessage, ex = unregistrationFailed)
            Left(unregistrationFailed)
        }
      case _ =>
        val unregistrationFailed = UnregistrationFailed(connection)
        log.error(unregistrationFailed.getMessage, ex = unregistrationFailed)
        Future.successful(Left(unregistrationFailed))
    }
  }

  /**
   * Unregister all connections from CRDT
   *
   * @note this method should be used for testing purpose only
   */
  def unregisterAll(): Future[Either[RegistrationListingFailed, Done]] =
    async {
      log.warn("Un-registering all components from location service")
      //Get all locations registered with CRDT
      val locations = await(list)

      //for each location unregister it's corresponding connection
      val dd = locations.map { locs =>
        Future.traverse(locs)(loc => unregister(loc.connection).flatMap(x => Future.fromTry(x.toTry)))
      }
      val ee = dd match {
        case Left(value)  => Future.successful(Left(value))
        case Right(value) => value.map(_ => Right(Done))
      }
      await(ee)
    }

  /**
   * Resolves the location for a connection from the local cache
   */
  def find[L <: Location](connection: TypedConnection[L]): Future[Either[RegistrationListingFailed, Option[L]]] = async {
    log.info(s"Finding location for connection: [${connection.name}]")
    await(list).map(x => x.find(_.connection == connection).asInstanceOf[Option[L]])
  }

  /**
   * Resolve a location for the given connection
   */
  def resolve[L <: Location](
      connection: TypedConnection[L],
      within: FiniteDuration
  ): Future[Either[RegistrationListingFailed, Option[L]]] = async {
    log.info(s"Resolving location for connection: [${connection.name}] within ${within.toString()}")
    val foundInLocalCache = await(find(connection))
    foundInLocalCache match {
      case Left(value) => Left(value)
      case Right(x)    => Right(if (x.isDefined) x else await(resolveWithin(connection, within)))
    }
  }

  /**
   * List all locations registered with CRDT
   */
  def list: Future[Either[RegistrationListingFailed, List[Location]]] = (replicator ? AllServices.get).map {
    case x @ GetSuccess(_)            => Right(x.get(AllServices.Key).entries.values.toList)
    case NotFound(AllServices.Key, _) => Right(List.empty)
    case _ =>
      val listingFailed = RegistrationListingFailed()
      log.error(listingFailed.getMessage, ex = listingFailed)
      Left(listingFailed)
  }

  /**
   * List all locations registered for the given componentType
   */
  def list(componentType: ComponentType): Future[Either[RegistrationListingFailed, List[Location]]] = async {
    await(list).map(x => x.filter(_.connection.componentId.componentType == componentType))
  }

  /**
   * List all locations registered with the given hostname
   */
  def list(hostname: String): Future[Either[RegistrationListingFailed, List[Location]]] = async {
    await(list).map(x => x.filter(_.uri.getHost == hostname))
  }

  /**
   * List all locations registered with the given connection type
   */
  def list(connectionType: ConnectionType): Future[Either[RegistrationListingFailed, List[Location]]] = async {
    await(list).map(x => x.filter(_.connection.connectionType == connectionType))
  }

  override def listByPrefix(_prefix: String): Future[Either[RegistrationListingFailed, List[AkkaLocation]]] = async {
    await(list).map { x =>
      x.collect {
        case akkaLocation: AkkaLocation if akkaLocation.prefix.toString.startsWith(_prefix) => akkaLocation
      }
    }
  }

  /**
   * Track the status of given connection
   */
  def track(connection: Connection): Source[TrackingEvent, Subscription] = {
    log.debug(s"Tracking connection: [${connection.name}]")
    //Create a message handler for this connection
    val service = new Registry.Service(connection)

    //Get a stream that emits messages sent to the actor generated after materialization
    val source = ActorSource
      .actorRef[Any](
        completionMatcher = PartialFunction.empty,
        failureMatcher = PartialFunction.empty,
        256,
        OverflowStrategy.dropHead
      )
      .mapMaterializedValue {
        //Subscribe materialized actorRef to the changes in connection so that above stream starts emitting messages
        actorRef =>
          replicator ! Replicator.Subscribe(service.Key, actorRef)
      }

    //Collect only the Changed events for this connection and transform it to location events.
    // If the changed event contains a Location, send LocationUpdated event.
    // If not, location must have been removed, send LocationRemoved event.
    val trackingEvents = source.collect {
      case c @ Changed(service.Key) if c.get(service.Key).value.isDefined =>
        LocationUpdated(c.get(service.Key).value.get)
      case c @ Changed(service.Key) => LocationRemoved(connection)
    }
    //Allow stream to be cancellable by giving it a KillSwitch in mat value.
    // Also, deduplicate identical messages in case multiple DeathWatch actors unregisters the same location.
    trackingEvents.cancellable.mapMaterializedValue(createSubscription).distinctUntilChanged
  }

  private def createSubscription(x: KillSwitch): Subscription = () => x.shutdown()

  /**
   * Subscribe to events of a connection by providing a callback.
   */
  override def subscribe(connection: Connection, callback: TrackingEvent => Unit): Subscription = {
    log.info(s"Subscribing to connection: [${connection.name}]")
    track(connection).to(Sink.foreach(callback)).run()
  }

  private def resolveWithin[L <: Location](connection: TypedConnection[L], waitTime: FiniteDuration): Future[Option[L]] =
    track(connection)
      .collect {
        case LocationUpdated(location) => location.asInstanceOf[L]
      }
      .takeWithin(waitTime)
      .runWith(Sink.headOption)
}
