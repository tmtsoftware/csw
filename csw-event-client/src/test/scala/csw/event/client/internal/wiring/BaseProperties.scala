package csw.event.client.internal.wiring

import java.net.URI

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer}
import akka.{actor, Done}
import csw.location.api.scaladsl.LocationService
import csw.event.api.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.event.client.helpers.RegistrationFactory
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.internal.commons.serviceresolver.EventServiceLocationResolver
import csw.event.client.internal.commons.{EventServiceConnection, EventStreamSupervisionStrategy}
import csw.location.api.commons.ClusterAwareSettings
import csw.location.scaladsl.LocationServiceFactory

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
  def createInfra(seedPort: Int, serverPort: Int): (actor.ActorSystem, LocationService) = {
    val system          = ClusterAwareSettings.onPort(seedPort).system
    val locationService = LocationServiceFactory.withSystem(system)
    val tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value, serverPort)
    locationService.register(tcpRegistration).await
    (system, locationService)
  }
}
