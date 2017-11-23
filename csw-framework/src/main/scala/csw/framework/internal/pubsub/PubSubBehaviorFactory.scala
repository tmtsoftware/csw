package csw.framework.internal.pubsub

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.messages.SupervisorMessage
import csw.messages.models.PubSub
import csw.services.logging.scaladsl.LoggerFactory

/**
 * Factory for creating [[akka.typed.scaladsl.Actor.MutableBehavior]] of a pub sub actor
 */
class PubSubBehaviorFactory() {
  def make[T](ctx: ActorContext[SupervisorMessage], actorName: String, loggerFactory: LoggerFactory): ActorRef[PubSub[T]] =
    ctx.spawn(Actor.mutable[PubSub[T]](ctx ⇒ new PubSubBehavior(ctx, loggerFactory)), actorName)
}
