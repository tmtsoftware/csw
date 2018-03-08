package csw.messages.framework

import akka.typed.ActorRef
import csw.messages.TMTSerializable

//TODO: what, why, how
sealed trait PubSub[T] extends TMTSerializable
object PubSub {
  sealed trait SubscriberMessage[T]           extends PubSub[T]
  case class Subscribe[T](ref: ActorRef[T])   extends SubscriberMessage[T]
  case class Unsubscribe[T](ref: ActorRef[T]) extends SubscriberMessage[T]

  sealed trait PublisherMessage[T] extends PubSub[T]
  case class Publish[T](data: T)   extends PublisherMessage[T]
}
