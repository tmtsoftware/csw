package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture

trait IEventService {
  val defaultPublisher: CompletableFuture[IEventPublisher]
  val defaultSubscriber: CompletableFuture[IEventSubscriber]

  def makeNewPublisher(): CompletableFuture[IEventPublisher]

}
