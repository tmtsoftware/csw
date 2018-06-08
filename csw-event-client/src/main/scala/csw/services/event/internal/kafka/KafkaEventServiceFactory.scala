package csw.services.event.internal.kafka

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import csw.services.event.internal.commons.EventServiceResolver
import csw.services.event.internal.commons.javawrappers.JEventService
import csw.services.event.javadsl.IEventService
import csw.services.event.scaladsl.EventService
import csw.services.location.javadsl.ILocationService
import csw.services.location.scaladsl.LocationService

import scala.concurrent.ExecutionContext

object KafkaEventServiceFactory {

  def make(locationService: LocationService)(implicit actorSystem: ActorSystem[_]): EventService = {

    implicit lazy val system: actor.ActorSystem = actorSystem.toUntyped
    implicit lazy val ec: ExecutionContext      = system.dispatcher
    lazy val settings                           = ActorMaterializerSettings(system).withSupervisionStrategy(Supervision.getResumingDecider)
    implicit lazy val resumingMat: Materializer = ActorMaterializer(settings)

    new KafkaEventService(new EventServiceResolver(locationService))
  }

  def jMake(locationService: ILocationService, actorSystem: ActorSystem[_]): IEventService = {
    implicit lazy val ec: ExecutionContext = actorSystem.executionContext
    new JEventService(make(locationService.asScala)(actorSystem))
  }
}
