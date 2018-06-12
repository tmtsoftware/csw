package csw.services.event.internal.commons
import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import csw.services.event.internal.commons.javawrappers.JEventService
import csw.services.event.internal.commons.serviceresolver.{
  EventServiceHostPortResolver,
  EventServiceLocationResolver,
  EventServiceResolver
}
import csw.services.event.javadsl.IEventService
import csw.services.event.scaladsl.EventService
import csw.services.location.javadsl.ILocationService
import csw.services.location.scaladsl.LocationService

import scala.concurrent.ExecutionContext

abstract class EventServiceFactory {
  protected def eventServiceImpl(eventServiceResolver: EventServiceResolver)(
      implicit actorSystem: actor.ActorSystem,
      ec: ExecutionContext,
      mat: Materializer
  ): EventService

  def make(locationService: LocationService)(implicit system: ActorSystem[_]): EventService = {
    implicit val untypedActorSystem: actor.ActorSystem = system.toUntyped
    implicit val ec: ExecutionContext                  = untypedActorSystem.dispatcher
    implicit val materializer: Materializer            = mat()
    eventServiceImpl(new EventServiceLocationResolver(locationService))
  }

  def make(host: String, port: Int)(implicit system: ActorSystem[_]): EventService = {
    implicit val untypedActorSystem: actor.ActorSystem = system.toUntyped
    implicit val ec: ExecutionContext                  = untypedActorSystem.dispatcher
    implicit val materializer: Materializer            = mat()
    eventServiceImpl(new EventServiceHostPortResolver(host, port))
  }

  def jMake(locationService: ILocationService, actorSystem: ActorSystem[_]): IEventService = {
    val eventService                  = make(locationService.asScala)(actorSystem)
    implicit val ec: ExecutionContext = actorSystem.executionContext
    new JEventService(eventService)
  }

  def jMake(host: String, port: Int, actorSystem: ActorSystem[_]): IEventService = {
    val eventService                  = make(host, port)(actorSystem)
    implicit val ec: ExecutionContext = actorSystem.executionContext
    new JEventService(eventService)
  }

  private def mat()(implicit actorSystem: actor.ActorSystem): Materializer = {
    val settings = ActorMaterializerSettings(actorSystem).withSupervisionStrategy(Supervision.getResumingDecider)
    ActorMaterializer(settings)
  }
}
