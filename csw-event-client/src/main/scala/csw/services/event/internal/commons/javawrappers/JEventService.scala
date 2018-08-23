package csw.services.event.internal.commons.javawrappers

import java.util.concurrent.CompletableFuture

import csw.services.event.api.javadsl.{IEventPublisher, IEventService}
import csw.services.event.api.scaladsl.EventService

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

/**
 * Java API for [[csw.services.event.api.scaladsl.EventService]]
 */
class JEventService(eventService: EventService) extends IEventService {

  implicit val executionContext: ExecutionContext = eventService.executionContext

  override def makeNewPublisher(): CompletableFuture[IEventPublisher] =
    eventService.makeNewPublisher().map[IEventPublisher](new JEventPublisher(_)).toJava.toCompletableFuture

  override def makeNewSubscriber(): JEventSubscriber = new JEventSubscriber(eventService.defaultSubscriber)

  override def asScala: EventService = eventService
}
