package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture

import csw.services.event.scaladsl.EventService

import scala.concurrent.ExecutionContext

trait IEventService {

  implicit val executionContext: ExecutionContext

  lazy val defaultPublisher: CompletableFuture[IEventPublisher]   = makeNewPublisher()
  lazy val defaultSubscriber: CompletableFuture[IEventSubscriber] = makeNewSubscriber()

  def makeNewPublisher(): CompletableFuture[IEventPublisher]
  def makeNewSubscriber(): CompletableFuture[IEventSubscriber]

  def asScala: EventService
}
