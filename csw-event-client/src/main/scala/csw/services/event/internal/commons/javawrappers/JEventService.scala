package csw.services.event.internal.commons.javawrappers

import java.util.concurrent.CompletableFuture

import csw.services.event.internal.commons.EventServiceAdapter
import csw.services.event.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.services.event.scaladsl.EventService

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

class JEventService(eventService: EventService)(implicit ec: ExecutionContext) extends IEventService {

  override val defaultPublisher: CompletableFuture[IEventPublisher] =
    eventService.defaultPublisher.map(EventServiceAdapter.asJava).toJava.toCompletableFuture

  override val defaultSubscriber: CompletableFuture[IEventSubscriber] =
    eventService.defaultSubscriber.map(EventServiceAdapter.asJava).toJava.toCompletableFuture

  override def makeNewPublisher(): CompletableFuture[IEventPublisher] =
    eventService.makeNewPublisher().map(EventServiceAdapter.asJava).toJava.toCompletableFuture
}
