package csw.services.location.internal

import akka.Done
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata._
import akka.pattern.ask
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.services.location.commons.CswCluster
import csw.services.location.exceptions.{OtherLocationIsRegistered, RegistrationFailed, RegistrationListingFailed, UnregistrationFailed}
import csw.services.location.internal.Registry.AllServices
import csw.services.location.internal.StreamExt.RichSource
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

/**
  * A `LocationService` implementation which manages registration data on akka cluster.
  *
  * The data is kept in two formats. One with [[akka.cluster.ddata.LWWRegister]] with `Connection.name`
  * as key and `Option[Location]` as value and
  *
  * the other with [[akka.cluster.ddata.LWWMap]] with a constant key and a map of `Connection` to `Location` as value
  *
  * @param cswCluster 'CswCluster' which gives handle to ActorSystem of akka cluster
  */
private[location] class LocationServiceImpl(cswCluster: CswCluster) extends LocationService { outer =>

  import cswCluster._
  implicit val timeout: Timeout = Timeout(5.seconds)


  /**
    * Registers a `Location` against connection name in `LWWRegister` and then `Connection` to `Location` in `LWWMap`.
    * A `Future` is returned with `Failure` :
    * {{{
    * - If the connection name is already present in LWWRegister with
    *   some other Location different than current Location
    *
    * - If update in LWWRegister fails, which will skip subsequent
    *   update in LWWMap as well
    *
    * - If update in LWWRegister is successful but LWWMap fails
    *   (This makes data inconsistent between LWWRegister and LWWMap. The
    *   user is expected to register again with same Registration so that
    *   LWWRegister will be updated again with same Option of Location and
    *   LWWMap will be updated with new entry of Connection to Location)
    * }}}
    *
    * If update in `LWWRegister` and `LWWMap` is successful then a `Future` is returned with `RegistrationResult`
    */
  def register(registration: Registration): Future[RegistrationResult] = {
    val location = registration.location(cswCluster.hostname)

    val service = new Registry.Service(location.connection)

    val updateValue = service.update {
      case r@LWWRegister(Some(`location`) | None) => r.withValue(Some(location))
      case LWWRegister(Some(otherLocation))       => throw OtherLocationIsRegistered(location, otherLocation)
    }

    val updateRegistry = AllServices.update(_ + (location.connection → location))

    (replicator ? updateValue).flatMap {
      case _: UpdateSuccess[_]                     => (replicator ? updateRegistry).map {
        case _: UpdateSuccess[_] => registrationResult(location)
        case _                   => throw RegistrationFailed(location.connection)
      }
      case ModifyFailure(service.Key, _, cause, _) => throw cause
      case _                                       => throw RegistrationFailed(location.connection)
    }
  }

  /**
    * Unregisters `Location` for `Connection` from `LWWRegister` and then from `LWWMap`.
    * A `Future` is returned with `Failure` :
    * {{{
    * - If update in LWWRegister fails, which will skip subsequent update in LWWMap
    *
    * - If update in LWWRegister is successful but in LWWMap fails
    *   (This makes data inconsistent between LWWRegister and LWWMap.
    *   The user is expected to unregister again with the same Connection
    *   so that LWWRegister will be updated again with None and
    *   LWWMap will be updated to remove entry of Connection to Location)
    * }}}
    *
    * If update in `LWWRegister` and `LWWMap` is successful then a `Future` is returned with `Success`
    */
  def unregister(connection: Connection): Future[Done] = {
    val service = new Registry.Service(connection)

    (replicator ? service.update(_.withValue(None))).flatMap {
      case x: UpdateSuccess[_] => (replicator ? AllServices.update(_ - connection)).map {
        case _: UpdateSuccess[_] => Done
        case _                   => throw UnregistrationFailed(connection)
      }
      case _                   => throw UnregistrationFailed(connection)
    }
  }

  /**
    * List all locations from `LWWMap` and unregister them one after another.
    *
    * A `Future` is returned with `Success` if all locations are unregistered successfully
    *
    * or with `Failure` if list from `LWWMap` fails or un-registration of any of the location fails
    */
  def unregisterAll(): Future[Done] = async {
    val locations = await(list)
    await(Future.traverse(locations)(loc ⇒ unregister(loc.connection)))
    Done
  }

  /**
    * List all entries from `LWWMap` and find a `Location` for the given `Connection`.
    *
    * A `Future` is returned with `None` if no location is found
    *
    * or with `Failure` if list from `LWWMap` fails
    */
  def resolve(connection: Connection): Future[Option[Location]] = async {
    await(list).find(_.connection == connection)
  }

  /**
    * List all entries from `LWWMap` and complete the `Future` with `Location` values.
    *
    * A `Future` is returned with empty list if no constant key is found for `LWWMap`.
    *
    * The returned `Future` will fail if list from `LWWMap` fails
    */
  def list: Future[List[Location]] = (replicator ? AllServices.get).map {
    case x@GetSuccess(AllServices.Key, _) => x.get(AllServices.Key).entries.values.toList
    case NotFound(AllServices.Key, _)     ⇒ List.empty
    case _                                => throw RegistrationListingFailed
  }

  /**
    * List all locations from `LWWMap` and complete the `Future` with `Location` values filtered on `ComponentType`.
    *
    * A `Future` is returned with empty list if no constant key is found for `LWWMap` or no locations are registered
    * against the given `ComponentType`.
    *
    * The returned `Future` will fail if list from `LWWMap` fails
    */
  def list(componentType: ComponentType): Future[List[Location]] = async {
    await(list).filter(_.connection.componentId.componentType == componentType)
  }

  /**
    * List all locations from `LWWMap` and complete the `Future` with `Location` values filtered on `Hostname`.
    *
    * A `Future` is returned with empty list if no constant key is found for `LWWMap` or no locations are registered
    * against the given `Hostname`.
    *
    * The returned `Future` will fail if list from `LWWMap` fails
    */
  def list(hostname: String): Future[List[Location]] = async {
    await(list).filter(_.uri.getHost == hostname)
  }

  /**
    * List all locations from `LWWMap` and complete the `Future` with `Location` values filtered on `ConnectionType`.
    *
    * A `Future` is returned with empty list if no constant key is found for `LWWMap` or no locations are registered
    * against the given `ComponentType`.
    *
    * The returned `Future` will fail if list from `LWWMap` fails
    */
  def list(connectionType: ConnectionType): Future[List[Location]] = async {
    await(list).filter(_.connection.connectionType == connectionType)
  }

  /**
    * Creates an `ActorRef` that subscribes for `Changed` messages for a given `Connection` from `LWWRegister` and pass
    * it to [[akka.stream.scaladsl.Source]].
    *
    * The `Source` will then map the `Changed` event to [[csw.services.location.models.LocationUpdated]]
    * if the `LWWRegister` contains the `Location` against `Connection`
    *
    * or to [[csw.services.location.models.LocationRemoved]] if there is no value against `Connection`.
    *
    * Un-track a given connection using [[akka.stream.KillSwitch]]
    *
    */
  def track(connection: Connection): Source[TrackingEvent, KillSwitch] = {
    val (source, actorRefF) = StreamExt.actorCoupling[Any]
    val service = new Registry.Service(connection)
    actorRefF.foreach(actorRef ⇒ replicator ! Subscribe(service.Key, actorRef))
    source.collect {
      case c@Changed(service.Key) if c.get(service.Key).value.isDefined => LocationUpdated(c.get(service.Key).value.get)
      case c@Changed(service.Key)                                       => LocationRemoved(connection)
    }.cancellable.distinctUntilChanged
  }

  /**
    * Terminate `ActorSystem` that was part of akka cluster.
    *
    * @note It is recommended not to perform any operation on `LocationService` after shutdown
    */
  def shutdown(): Future[Done] = cswCluster.terminate()

  private def registrationResult(loc: Location): RegistrationResult = new RegistrationResult {
    override def location: Location = loc

    override def unregister(): Future[Done] = outer.unregister(location.connection)
  }
}
