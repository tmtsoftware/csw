package csw.event.client.internal.wiring

import java.net.URI

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.stream.{ActorMaterializerSettings, Materializer}
import csw.event.api.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.internal.commons.serviceresolver.EventServiceLocationResolver
import csw.event.client.internal.commons.{EventServiceConnection, EventStreamSupervisionStrategy}
import csw.location.api.models.TcpRegistration
import csw.location.api.scaladsl.LocationService
import csw.location.client.internal.LocationServiceClient

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

  implicit val actorSystem: ActorSystem[_]
  implicit lazy val ec: ExecutionContext = actorSystem.executionContext
  lazy val settings: ActorMaterializerSettings =
    ActorMaterializerSettings(actorSystem.toUntyped).withSupervisionStrategy(EventStreamSupervisionStrategy.decider)
  implicit lazy val resumingMat: Materializer = ActorMaterializer(Some(settings))

  def resolveEventService(locationService: LocationService): Future[URI] = async {
    val eventServiceResolver = new EventServiceLocationResolver(locationService)
    await(eventServiceResolver.uri())
  }
}

object BaseProperties {
  def createInfra(serverPort: Int, httpPort: Int): (LocationService, ActorSystem[_]) = {

    implicit val typedSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "event-server")
    implicit val mat: Materializer           = ActorMaterializer()

    val locationService = new LocationServiceClient("localhost", httpPort)
    val tcpRegistration = TcpRegistration(EventServiceConnection.value, serverPort)

    locationService.register(tcpRegistration).await
    (locationService, typedSystem)
  }
}
