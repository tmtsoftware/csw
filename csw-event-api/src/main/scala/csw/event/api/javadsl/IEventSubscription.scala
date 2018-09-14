package csw.event.api.javadsl

import java.util.concurrent.CompletableFuture

import akka.Done

/**
 * An interface to represent a subscription. On subscribing to one or more Event Keys using the [[csw.event.api.javadsl.IEventSubscriber]],
 * the subscriber gets a handle to that particular subscription so as to perform some subscription specific tasks.
 */
trait IEventSubscription {

  /**
   * To unsubscribe a given subscription. This will also clean up subscription specific underlying resources
   * @return a future which completes when the unsubscribe is completed
   */
  def unsubscribe(): CompletableFuture[Done]

  /**
   * To check if the underlying subscription is ready to emit elements
   * @return a future which completes when the underlying subscription is ready to emit elements
   */
  def ready(): CompletableFuture[Done]
}
