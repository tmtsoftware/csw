package csw.services.event.scaladsl

import akka.Done

import scala.concurrent.Future

/**
 * An interface to represent a subscription. On subscribing to one or more Event Keys using the [[csw.services.event.scaladsl.EventSubscriber]],
 * the subscriber gets a handle to that particular subscription so as to perform some subscription specific tasks.
 */
trait EventSubscription {

  /**
   * To unsubscribe a given subscription. This will also clean up subscription specific underlying resources
   * @return a future which completes when the unsubscribe is completed
   */
  def unsubscribe(): Future[Done]

  /**
   * To check if the underlying subscription is ready to emit elements
   * @return a future which completes when the underlying subscription is ready to emit elements
   */
  def ready(): Future[Done]
}
