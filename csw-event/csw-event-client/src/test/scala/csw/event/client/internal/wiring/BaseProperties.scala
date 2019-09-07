package csw.event.client.internal.wiring

import java.net.URI

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.{ActorAttributes, Attributes, Materializer}
import csw.event.api.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.internal.commons.{EventServiceConnection, EventStreamSupervisionStrategy}
import csw.event.client.internal.commons.serviceresolver.EventServiceLocationResolver
import csw.location.api.scaladsl.LocationService
import csw.location.client.internal.LocationServiceClient
import csw.location.models.TcpRegistration

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
  val attributes: Attributes             = ActorAttributes.supervisionStrategy(EventStreamSupervisionStrategy.decider)

  def resolveEventService(locationService: LocationService): Future[URI] = async {
    val eventServiceResolver = new EventServiceLocationResolver(locationService)
    await(eventServiceResolver.uri())
  }
}

object BaseProperties {
  def createInfra(serverPort: Int, httpPort: Int): (LocationService, ActorSystem[Nothing]) = {

    implicit val typedSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "event-server")
    implicit val mat: Materializer                 = Materializer(typedSystem)

    val locationService = new LocationServiceClient("localhost", httpPort)
    val tcpRegistration = TcpRegistration(EventServiceConnection.value, serverPort)

    locationService.register(tcpRegistration).await
    (locationService, typedSystem)
  }
}
