package csw.common.framework.internal.pubsub

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.common.framework.models.{PubSub, SupervisorMessage}

class PubSubBehaviorFactory() {
  def make[T](ctx: ActorContext[SupervisorMessage], actorName: String, componentName: String): ActorRef[PubSub[T]] =
    ctx.spawn(Actor.mutable[PubSub[T]](ctx ⇒ new PubSubBehavior(ctx, componentName)), actorName)
}
