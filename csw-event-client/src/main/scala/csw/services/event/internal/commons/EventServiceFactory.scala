package csw.services.event.internal.commons
import akka.actor.ActorSystem
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

/**
 * Base class for factory instances of EventService
 */
abstract class EventServiceFactory {

  protected def eventServiceImpl(eventServiceResolver: EventServiceResolver)(
      implicit actorSystem: ActorSystem,
      mat: Materializer
  ): EventService

  def make(locationService: LocationService)(implicit system: ActorSystem): EventService = {
    implicit val ec: ExecutionContext       = system.dispatcher
    implicit val materializer: Materializer = mat()
    eventServiceImpl(new EventServiceLocationResolver(locationService))
  }

  def make(host: String, port: Int)(implicit system: ActorSystem): EventService =
    eventServiceImpl(new EventServiceHostPortResolver(host, port))(system, mat())

  def jMake(locationService: ILocationService, actorSystem: ActorSystem): IEventService = {
    val eventService = make(locationService.asScala)(actorSystem)
    new JEventService(eventService)
  }

  def jMake(host: String, port: Int, actorSystem: ActorSystem): IEventService = {
    val eventService = make(host, port)(actorSystem)
    new JEventService(eventService)
  }

  private def mat()(implicit actorSystem: ActorSystem): Materializer = {
    val settings = ActorMaterializerSettings(actorSystem).withSupervisionStrategy(Supervision.getResumingDecider)
    ActorMaterializer(settings)
  }
}
