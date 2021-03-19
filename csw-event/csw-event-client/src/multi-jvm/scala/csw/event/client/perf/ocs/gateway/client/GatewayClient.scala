package csw.event.client.perf.ocs.gateway.client

import akka.NotUsed
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitches, UniqueKillSwitch}
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.core.formats.JsonSupport
import csw.params.events.{Event, EventKey}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.constants.CommonTimeouts
import esw.gateway.api.clients.EventClient
import esw.gateway.api.codecs.GatewayCodecs._
import esw.gateway.api.protocol.{GatewayRequest, GatewayStreamRequest}
import msocket.api.ContentType
import msocket.http.post.HttpPostTransport
import msocket.http.ws.WebsocketTransport
import org.scalatest.OptionValues.convertOptionToValuable
import play.api.libs.json.Json

import scala.async.Async._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class GatewayClient(subId: Int)(implicit val actorSystem: ActorSystem[SpawnProtocol.Command]) {

  import actorSystem.executionContext
  implicit val scheduler: Scheduler                 = actorSystem.scheduler
  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  private val gatewayPrefix                         = Prefix(ESW, "EswGateway")
  private val gatewayLocation                       = resolveHTTPLocation(gatewayPrefix, Service)
  private val baseUri                               = gatewayLocation.uri.toString + "events"
  private val serverIp                              = gatewayLocation.uri.getHost
  private val serverPort                            = gatewayLocation.uri.getPort

  lazy val eventClient: EventClient = new EventClient(gatewayHTTPClient(), gatewayWebSocketClient)

  def subscribe(keys: Set[EventKey]): Source[Event, UniqueKillSwitch] = {
    eventClient.subscribe(keys, None).viaMat(KillSwitches.single)(Keep.right)
  }

  private def gatewayHTTPClient(tokenFactory: () => Option[String] = () => None) = {
    val httpUri = Uri(s"http://$serverIp:$serverPort").withPath(Path("/post-endpoint")).toString()
    new HttpPostTransport[GatewayRequest](httpUri, ContentType.Json, tokenFactory)
  }

  private def gatewayWebSocketClient = {
    val webSocketUri = Uri(s"http://$serverIp:$serverPort").withScheme("ws").withPath(Path("/websocket-endpoint")).toString()
    val appName      = Some(s"Subscriber-$subId")
    new WebsocketTransport[GatewayStreamRequest](webSocketUri, ContentType.Json, () => None, appName)
  }

  def subscribe(eventKey: EventKey): Source[Event, UniqueKillSwitch] = {
    val subsystem = eventKey.source.subsystem
    val prefix    = eventKey.source
    val component = prefix.componentName

    val uri     = Uri(s"$baseUri/subscribe/$subsystem?component=$component&event=${eventKey.eventName.name}")
    val request = HttpRequest(HttpMethods.GET, uri = uri)

    val sseStreamFuture: Future[Source[ServerSentEvent, NotUsed]] = async {
      val response              = await(Http().singleRequest(request))
      implicit val unmarshaller = fromEventsStream(actorSystem.toClassic)
      await(Unmarshal(response.entity).to[Source[ServerSentEvent, NotUsed]])
    }

    val sseStream = Source.future(sseStreamFuture).flatMapConcat(identity)
    sseStream.map(x => JsonSupport.reads[Event](Json.parse(x.data))).viaMat(KillSwitches.single)(Keep.right)
  }

  private def resolveHTTPLocation(prefix: Prefix, componentType: ComponentType) = {
    val gatewayConnection = HttpConnection(ComponentId(prefix, componentType))
    Await.result(locationService.resolve(gatewayConnection, CommonTimeouts.ResolveLocation).map(_.value), 60.seconds)
  }
}
