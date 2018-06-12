package csw.services.location.internal

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Source
import akka.stream.{KillSwitch, Materializer}
import csw.messages.location._
import csw.services.location.javadsl.ILocationService
import csw.services.location.models.{Registration, RegistrationResult}
import csw.services.location.scaladsl.LocationService
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import io.circe.syntax._

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class LocationServiceClient(implicit actorSystem: ActorSystem, mat: Materializer)
    extends LocationService
    with FailFastCirceSupport
    with LocationJsonSupport { outer =>

  import actorSystem.dispatcher

  override def register(registration: Registration): Future[RegistrationResult] = async {
    val uri            = Uri("http://localhost:7654/location/register")
    val requestEntity  = await(Marshal(registration).to[RequestEntity])
    val request        = HttpRequest(HttpMethods.POST, uri = uri, entity = requestEntity)
    val responseEntity = await(Http().singleRequest(request)).entity
    val location0      = await(Unmarshal(responseEntity).to[Location])
    new RegistrationResult {
      override def unregister(): Future[Done] = outer.unregister(location0.connection)
      override def location: Location         = location0
    }
  }

  override def unregister(connection: Connection): Future[Done] = async {
    val uri            = Uri("http://localhost:7654/location/unregister")
    val requestEntity  = await(Marshal(connection).to[RequestEntity])
    val request        = HttpRequest(HttpMethods.POST, uri = uri, entity = requestEntity)
    val responseEntity = await(Http().singleRequest(request)).entity
    await(Unmarshal(responseEntity).to[Done])
  }

  override def unregisterAll(): Future[Done] = async {
    val uri            = Uri("http://localhost:7654/location/unregisterAll")
    val request        = HttpRequest(HttpMethods.POST, uri = uri)
    val responseEntity = await(Http().singleRequest(request)).entity
    await(Unmarshal(responseEntity).to[Done])
  }

  override def find[L <: Location](connection: TypedConnection[L]): Future[Option[L]] = async {
    val uri            = Uri(s"http://localhost:7654/location/find/${connection.name}")
    val request        = HttpRequest(HttpMethods.GET, uri = uri)
    val responseEntity = await(Http().singleRequest(request)).entity
    await(Unmarshal(responseEntity).to[Option[Location]]).map(_.asInstanceOf[L])
  }

  override def resolve[L <: Location](connection: TypedConnection[L], within: FiniteDuration): Future[Option[L]] = async {
    val uri            = Uri(s"http://localhost:7654/location/resolve/${connection.name}?within=$within")
    val request        = HttpRequest(HttpMethods.GET, uri = uri)
    val responseEntity = await(Http().singleRequest(request)).entity
    await(Unmarshal(responseEntity).to[Option[Location]]).map(_.asInstanceOf[L])
  }

  override def list: Future[List[Location]] = async {
    val uri            = Uri("http://localhost:7654/location/list")
    val request        = HttpRequest(HttpMethods.GET, uri = uri)
    val responseEntity = await(Http().singleRequest(request)).entity
    await(Unmarshal(responseEntity).to[List[Location]])
  }

  override def list(componentType: ComponentType): Future[List[Location]] = async {
    val uri            = Uri(s"http://localhost:7654/location/list?componentType=$componentType")
    val request        = HttpRequest(HttpMethods.GET, uri = uri)
    val responseEntity = await(Http().singleRequest(request)).entity
    await(Unmarshal(responseEntity).to[List[Location]])
  }

  override def list(hostname: String): Future[List[Location]] = async {
    val uri            = Uri(s"http://localhost:7654/location/list/hostname=$hostname")
    val request        = HttpRequest(HttpMethods.GET, uri = uri)
    val responseEntity = await(Http().singleRequest(request)).entity
    await(Unmarshal(responseEntity).to[List[Location]])
  }

  override def list(connectionType: ConnectionType): Future[List[Location]] = async {
    val uri            = Uri(s"http://localhost:7654/location/list/connectionType=$connectionType")
    val request        = HttpRequest(HttpMethods.GET, uri = uri)
    val responseEntity = await(Http().singleRequest(request)).entity
    await(Unmarshal(responseEntity).to[List[Location]])
  }

  override def listByPrefix(prefix: String): Future[List[AkkaLocation]] = async {
    val uri            = Uri(s"http://localhost:7654/location/list/prefix=$prefix")
    val request        = HttpRequest(HttpMethods.GET, uri = uri)
    val responseEntity = await(Http().singleRequest(request)).entity
    await(Unmarshal(responseEntity).to[List[AkkaLocation]])
  }

  /**
   * Tracks the connection and send events for modification or removal of its location
   *
   * @param connection the `connection` that is to be tracked
   * @return A stream that emits events related to the connection. It can be cancelled using KillSwitch. This will stop giving
   *         events for earlier tracked connection
   */
  override def track(connection: Connection): Source[TrackingEvent, KillSwitch] = ???

  /**
   * Subscribe to tracking events for a connection by providing a callback
   * For each event the callback is invoked.
   * Use this method if you do not want to handle materialization and happy with a side-effecting callback instead
   *
   * @param connection the `connection` that is to be tracked
   * @param callback   the callback function of type `TrackingEvent` => Unit which gets executed on receiving any `TrackingEvent`
   * @return a killswitch which can be shutdown to unsubscribe the consumer
   */
  override def subscribe(connection: Connection, callback: TrackingEvent => Unit): KillSwitch = ???

  /**
   * Shuts down the LocationService
   *
   * @see terminate method in [[csw.services.location.commons.CswCluster]]
   * @note it is recommended not to perform any operation on LocationService after calling this method
   * @param reason the reason explaining the shutdown
   * @return a future which completes when the location service has shutdown successfully
   */
  override def shutdown(reason: CoordinatedShutdown.Reason): Future[Done] = ???

  /**
   * Returns the Java API for this instance of location service
   */
  override def asJava: ILocationService = ???
}
