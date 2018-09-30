package csw.event.client.internal.wiring

import java.net.URI

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer}
import akka.{actor, Done}
import com.typesafe.config.{Config, ConfigFactory}
import csw.event.api.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.internal.commons.serviceresolver.EventServiceLocationResolver
import csw.event.client.internal.commons.{EventServiceConnection, EventStreamSupervisionStrategy}
import csw.location.api.models.TcpRegistration
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

trait BaseProperties {
  val eventPattern: String
  val publisher: EventPublisher
  val subscriber: EventSubscriber
  val eventService: EventService
  val jEventService: IEventService
  val jPublisher: IEventPublisher
  val jSubscriber: IEventSubscriber
  def publishGarbage(channel: String, message: String): Future[Done]
  def start(): Unit
  def shutdown(): Unit

  implicit val actorSystem: actor.ActorSystem
  implicit val typedActorSystem: ActorSystem[_] = actorSystem.toTyped
  implicit lazy val ec: ExecutionContext        = actorSystem.dispatcher
  lazy val settings: ActorMaterializerSettings =
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(EventStreamSupervisionStrategy.decider)
  implicit lazy val resumingMat: Materializer = ActorMaterializer(settings)

  def resolveEventService(locationService: LocationService): Future[URI] = async {
    val eventServiceResolver = new EventServiceLocationResolver(locationService)
    await(eventServiceResolver.uri())
  }
}

object BaseProperties {
  def createInfra(serverPort: Int, httpPort: Int): (LocationService, actor.ActorSystem) = {

    val httpPortConfig                     = ConfigFactory.parseString("csw-cluster-seed.http-location-port=" + httpPort)
    val config: Config                     = ConfigFactory.load(httpPortConfig.withFallback(ConfigFactory.load()))
    implicit val system: actor.ActorSystem = actor.ActorSystem("event-server", config)
    implicit val mat: ActorMaterializer    = ActorMaterializer()

    val locationService = HttpLocationServiceFactory.makeLocalClient
    val tcpRegistration = TcpRegistration(EventServiceConnection.value, serverPort)

    locationService.register(tcpRegistration).await
    (locationService, system)
  }
}
