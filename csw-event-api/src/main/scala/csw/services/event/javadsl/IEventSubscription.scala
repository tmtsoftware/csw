package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture

import akka.Done

trait IEventSubscription {
  def unsubscribe(): CompletableFuture[Done]
}
