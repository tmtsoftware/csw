package csw.location.client.internal

import java.io.IOException

import akka.actor.typed.{ActorSystem, Scheduler}
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.CoordinatedShutdown
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitch, KillSwitches, Materializer}
import akka.{Done, NotUsed, actor}
import csw.location.api.codec.DoneCodec
import csw.location.api.exceptions.{OtherLocationIsRegistered, RegistrationFailed}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.HttpCodecs
import csw.location.models.{
  AkkaLocation,
  ComponentType,
  Connection,
  ConnectionType,
  Location,
  Registration,
  TrackingEvent,
  TypedConnection
}
import csw.location.models.codecs.LocationCodecs
import io.bullet.borer.Json

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

private[csw] class LocationServiceClient(serverIp: String, serverPort: Int)(
    implicit val actorSystem: ActorSystem[_],
    mat: Materializer
) extends LocationService
    with HttpCodecs
    with LocationCodecs
    with DoneCodec { outer =>

  import actorSystem.executionContext
  implicit val untypedSystem: actor.ActorSystem = actorSystem.toClassic
  implicit val scheduler: Scheduler             = actorSystem.scheduler

  private val baseUri = s"http://$serverIp:$serverPort/location"

  override def register(registration: Registration): Future[RegistrationResult] = async {
    val uri           = Uri(baseUri + "/register")
    val requestEntity = await(Marshal(registration).to[RequestEntity])
    val request       = HttpRequest(HttpMethods.POST, uri = uri, entity = requestEntity)
    val response      = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK =>
        val location0 = await(Unmarshal(response.entity).to[Location])
        CoordinatedShutdown(untypedSystem).addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "unregister")(
          () => unregister(location0.connection)
        )
        new RegistrationResult {
          override def unregister(): Future[Done] = outer.unregister(location0.connection)
          override def location: Location         = location0
        }
      //fixme: status code 400 is wrongly mapped to OtherLocationIsRegistered
      case x @ StatusCodes.BadRequest          => throw OtherLocationIsRegistered(x.reason)
      case x @ StatusCodes.InternalServerError => throw RegistrationFailed(x.reason)
      case _                                   => await(throwExOnInvalidResponse[RegistrationResult](request, response))
    }
  }

  override def unregister(connection: Connection): Future[Done] = async {
    val uri           = Uri(baseUri + "/unregister")
    val requestEntity = await(Marshal(connection).to[RequestEntity])
    val request       = HttpRequest(HttpMethods.POST, uri = uri, entity = requestEntity)
    val response      = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[Done])
  }

  override def unregisterAll(): Future[Done] = async {
    val uri      = Uri(baseUri + "/unregisterAll")
    val request  = HttpRequest(HttpMethods.POST, uri = uri)
    val response = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[Done])
  }

  override def find[L <: Location](connection: TypedConnection[L]): Future[Option[L]] = async {
    val uri      = Uri(s"$baseUri/find/${connection.name}")
    val request  = HttpRequest(HttpMethods.GET, uri = uri)
    val response = await(Http().singleRequest(request))
    response.status match {
      case StatusCodes.OK       => Some(await(Unmarshal(response.entity).to[Location]).asInstanceOf[L])
      case StatusCodes.NotFound => None
      case _                    => await(throwExOnInvalidResponse[Option[L]](request, response))
    }
  }

  override def resolve[L <: Location](connection: TypedConnection[L], within: FiniteDuration): Future[Option[L]] = async {
    val uri = Uri(
      s"$baseUri/resolve/${connection.name}?within=${within.length.toString + within.unit.toString.toLowerCase}"
    )
    val request  = HttpRequest(HttpMethods.GET, uri = uri)
    val response = await(Http().singleRequest(request))
    response.status match {
      case StatusCodes.OK       => Some(await(Unmarshal(response.entity).to[Location]).asInstanceOf[L])
      case StatusCodes.NotFound => None
      case _                    => await(throwExOnInvalidResponse[Option[L]](request, response))
    }
  }

  override def list: Future[List[Location]] = async {
    val uri      = Uri(baseUri + "/list")
    val request  = HttpRequest(HttpMethods.GET, uri = uri)
    val response = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[List[Location]])
  }

  override def list(componentType: ComponentType): Future[List[Location]] = async {
    val uri      = Uri(s"$baseUri/list?componentType=$componentType")
    val request  = HttpRequest(HttpMethods.GET, uri = uri)
    val response = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[List[Location]])
  }

  override def list(hostname: String): Future[List[Location]] = async {
    val uri      = Uri(s"$baseUri/list?hostname=$hostname")
    val request  = HttpRequest(HttpMethods.GET, uri = uri)
    val response = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[List[Location]])
  }

  override def list(connectionType: ConnectionType): Future[List[Location]] = async {
    val uri      = Uri(s"$baseUri/list?connectionType=${connectionType.entryName}")
    val request  = HttpRequest(HttpMethods.GET, uri = uri)
    val response = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[List[Location]])
  }

  override def listByPrefix(prefix: String): Future[List[AkkaLocation]] = async {
    val uri      = Uri(s"$baseUri/list?prefix=$prefix")
    val request  = HttpRequest(HttpMethods.GET, uri = uri)
    val response = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[List[AkkaLocation]])
  }

  override def track(connection: Connection): Source[TrackingEvent, KillSwitch] = {
    val uri     = Uri(s"$baseUri/track/${connection.name}")
    val request = HttpRequest(HttpMethods.GET, uri = uri)
    val sseStreamFuture = async {
      val response = await(Http().singleRequest(request))
      await(Unmarshal(response.entity).to[Source[ServerSentEvent, NotUsed]])
    }
    val sseStream = Source.future(sseStreamFuture).flatMapConcat(identity)
    sseStream
      .map { x =>
        Json.decode(x.data.getBytes("utf8")).to[TrackingEvent].value
      }
      .viaMat(KillSwitches.single)(Keep.right)
  }

  override def subscribe(connection: Connection, callback: TrackingEvent => Unit): KillSwitch =
    track(connection).to(Sink.foreach(callback)).run()

  private def throwExOnInvalidResponse[T](request: HttpRequest, response: HttpResponse): Future[T] =
    Future.failed(
      new IOException(s"""Request failed with response status: [${response.status}]
           |Requested URI: [${request.uri}] and
           |Response body: ${response.entity}""".stripMargin)
    )
}
