package csw.services.event.internal.wiring

import java.net.URI

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import csw.services.event.helpers.RegistrationFactory
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.commons.{EventServiceConnection, EventServiceResolver}
import csw.services.event.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.services.event.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

trait BaseProperties {
  val eventPattern: String
  def publisher: EventPublisher
  def subscriber: EventSubscriber
  def eventService: EventService
  def jEventService: IEventService
  def jPublisher[T <: EventPublisher]: IEventPublisher
  def jSubscriber[T <: EventSubscriber]: IEventSubscriber
  def start(): Unit
  def shutdown(): Unit

  implicit val actorSystem: actor.ActorSystem
  implicit val typedActorSystem: ActorSystem[_] = actorSystem.toTyped
  implicit lazy val ec: ExecutionContext        = actorSystem.dispatcher
  lazy val settings: ActorMaterializerSettings =
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(Supervision.getResumingDecider)
  implicit lazy val resumingMat: Materializer = ActorMaterializer(settings)

  def resolveEventService(locationService: LocationService): Future[URI] = async {
    val eventServiceResolver = new EventServiceResolver(locationService)
    await(eventServiceResolver.uri)
  }
}

object BaseProperties {
  def createInfra(seedPort: Int, serverPort: Int): (ClusterSettings, LocationService) = {
    val clusterSettings: ClusterSettings = ClusterAwareSettings.joinLocal(seedPort)
    val locationService                  = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
    val tcpRegistration                  = RegistrationFactory.tcp(EventServiceConnection.value, serverPort)
    locationService.register(tcpRegistration).await
    (clusterSettings, locationService)
  }
}
