package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture

import csw.services.event.scaladsl.EventService

import scala.concurrent.ExecutionContext

trait IEventService {
  val executionContext: ExecutionContext
  val defaultPublisher: CompletableFuture[IEventPublisher]
  val defaultSubscriber: CompletableFuture[IEventSubscriber]

  def makeNewPublisher(): CompletableFuture[IEventPublisher]

  def asScala: EventService
}
