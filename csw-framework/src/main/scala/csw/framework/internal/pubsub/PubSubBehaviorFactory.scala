package csw.framework.internal.pubsub

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.messages.commands.Nameable
import csw.messages.framework.PubSub
import csw.services.logging.scaladsl.LoggerFactory

/**
 * Factory for creating [[akka.actor.typed.scaladsl.MutableBehavior]] of a pub sub actor
 */
private[framework] class PubSubBehaviorFactory() {
  def make[T: Nameable, U: Nameable](actorName: String, loggerFactory: LoggerFactory): Behavior[PubSub[T, U]] =
    Behaviors.setup[PubSub[T, U]](ctx ⇒ new PubSubBehavior(ctx, loggerFactory))
}
