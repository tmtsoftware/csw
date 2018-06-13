package csw.services.location.internal

import akka.actor.{ActorSystem, CoordinatedShutdown, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{KillSwitch, Materializer}
import akka.{Done, NotUsed}
import csw.messages.location._
import csw.services.location.internal.StreamExt.RichSource
import csw.services.location.javadsl.ILocationService
import csw.services.location.models.{Registration, RegistrationResult}
import csw.services.location.scaladsl.LocationService
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import io.circe.parser._
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class LocationServiceClient(implicit actorSystem: ActorSystem, mat: Materializer)
    extends LocationService
    with FailFastCirceSupport
    with LocationJsonSupport { outer =>

  import actorSystem.dispatcher
  implicit val scheduler: Scheduler = actorSystem.scheduler

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

  override def track(connection: Connection): Source[TrackingEvent, KillSwitch] = {
    val uri     = Uri(s"http://localhost:7654/location/track/${connection.name}")
    val request = HttpRequest(HttpMethods.GET, uri = uri)
    val sseStreamFuture = async {
      val responseEntity = await(Http().singleRequest(request)).entity
      await(Unmarshal(responseEntity).to[Source[ServerSentEvent, NotUsed]])
    }
    val sseStream = Source.fromFuture(sseStreamFuture).flatMapConcat(identity)
    sseStream.map(x => decode[TrackingEvent](x.data).right.get).cancellable
  }

  override def subscribe(connection: Connection, callback: TrackingEvent => Unit): KillSwitch = {
    track(connection).to(Sink.foreach(callback)).run()
  }

  override def shutdown(reason: CoordinatedShutdown.Reason): Future[Done] =
    Future.failed(new RuntimeException("can not shutdown via http-client"))

  override def asJava: ILocationService = new JLocationServiceImpl(this)
}
