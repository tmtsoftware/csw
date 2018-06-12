package csw.services.event.internal.commons.javawrappers

import java.util.concurrent.CompletableFuture

import csw.services.event.internal.commons.EventServiceAdapter
import csw.services.event.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.services.event.scaladsl.EventService

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

class JEventService(eventService: EventService)(implicit val executionContext: ExecutionContext) extends IEventService {

  override lazy val defaultPublisher: CompletableFuture[IEventPublisher] =
    eventService.defaultPublisher.map(EventServiceAdapter.asJava).toJava.toCompletableFuture

  override lazy val defaultSubscriber: CompletableFuture[IEventSubscriber] =
    eventService.defaultSubscriber.map(EventServiceAdapter.asJava).toJava.toCompletableFuture

  override def makeNewPublisher(): CompletableFuture[IEventPublisher] =
    eventService.makeNewPublisher().map(EventServiceAdapter.asJava).toJava.toCompletableFuture

  override def asScala: EventService = eventService
}
