package csw.event.client.perf.ocs.gateway.client
import akka.NotUsed
import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{ActorMaterializer, KillSwitches, Materializer, UniqueKillSwitch}
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.perf.utils.EventUtils
import csw.location.client.ActorSystemFactory
import csw.params.core.formats.JsonSupport
import csw.params.core.models.Prefix
import csw.params.events.{Event, EventKey, EventName}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.Json

import scala.async.Async._

class GatewayClient(serverIp: String, port: Int)(implicit val actorSystem: ActorSystem, mat: Materializer)
    extends PlayJsonSupport
    with JsonSupport {

  import actorSystem.dispatcher
  implicit val scheduler: Scheduler = actorSystem.scheduler

  private val baseUri = s"http://$serverIp:$port/events"

  def subscribe(keys: Set[EventKey]): Source[Event, UniqueKillSwitch] =
    keys.map(subscribe).reduce(_ merge _)

  def subscribe(eventKey: EventKey): Source[Event, UniqueKillSwitch] = {
    val subsystem = eventKey.source.subsystem
    val prefix    = eventKey.source.prefix
    val component = prefix.splitAt(prefix.indexOf(".") + 1)._2

    val uri     = Uri(s"$baseUri/subscribe/$subsystem?component=$component&event=${eventKey.eventName.name}")
    val request = HttpRequest(HttpMethods.GET, uri = uri)

    val sseStreamFuture = async {
      val response = await(Http().singleRequest(request))
      await(Unmarshal(response.entity).to[Source[ServerSentEvent, NotUsed]])
    }

    val sseStream = Source.fromFuture(sseStreamFuture).flatMapConcat(identity)
    sseStream.map(x ⇒ Json.parse(x.data).as[Event]).viaMat(KillSwitches.single)(Keep.right)
  }

}

object Main extends App {

  private implicit val system: ActorSystem    = ActorSystemFactory.remote()
  private implicit val mat: ActorMaterializer = ActorMaterializer()

  private val client = new GatewayClient("localhost", 9090)

  private val eventName = EventName("gateway")
  private val prefix    = Prefix("tcs.test")

  (1 to 50).map { _ ⇒
    client
      .subscribe(EventKey(prefix, eventName))
      .runForeach(println)
  }

  private val factory                    = new EventServiceFactory()
  private val eventService: EventService = factory.make("localhost", 26379)

  Thread.sleep(1000)
  eventService.defaultPublisher.publish(
    EventUtils.event(eventName, prefix, 2)
  )
}
