package csw.command.client.internal.models.framework

import akka.actor.typed.ActorRef
import csw.params.core.states.StateName
import csw.serializable.TMTSerializable

/**
 * Represents the protocol or messages about publishing data and subscribing it
 *
 * @tparam T represents the type of data that is published or subscribed
 */
sealed trait PubSub[T] extends TMTSerializable
object PubSub {

  /**
   * Represents the messages about subscribing data e.g Subscribe and Unsubscribe
   *
   * @tparam T represents the type of data that is subscribed
   */
  sealed trait SubscriberMessage[T] extends PubSub[T]

  /**
   * Represents the subscribe action where all the current state publishing from the particular component can be subscribed to
   *
   * @param ref the reference of subscriber used to notify to when some data is published
   * @tparam T represents the type of data that is subscribed
   */
  case class Subscribe[T](ref: ActorRef[T]) extends SubscriberMessage[T]

  /**
   * Represents the subscribe action for current states specified by a set of stateNames
   *
   * @param ref the reference of subscriber used to notify to when some data is published
   * @param names set of stateNames uniquely representating current states for a component
   * @tparam T represents the type of data that is subscribed
   */
  case class SubscribeOnly[T](ref: ActorRef[T], names: Set[StateName]) extends SubscriberMessage[T]

  /**
   * Represents a unsubscribe action
   *
   * @param ref the reference of subscriber that no longer wishes to receive notification for published data
   * @tparam T represents the type of data that is subscribed
   */
  case class Unsubscribe[T](ref: ActorRef[T]) extends SubscriberMessage[T]

  /**
   * Represents the messages about publishing data e.g Publish
   *
   * @tparam T represents the type of data that is published
   */
  sealed trait PublisherMessage[T] extends PubSub[T]

  /**
   * Represents a publish action
   *
   * @param data of type T that gets published
   * @tparam T represents the type of data that is published
   */
  case class Publish[T](data: T) extends PublisherMessage[T]
}
