package csw.framework.internal.pubsub

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.params.commands.Nameable
import csw.command.client.models.framework.PubSub
import csw.logging.scaladsl.LoggerFactory

/**
 * Factory for creating [[akka.actor.typed.scaladsl.MutableBehavior]] of a pub sub actor
 */
private[framework] class PubSubBehaviorFactory() {
  def make[T: Nameable](actorName: String, loggerFactory: LoggerFactory): Behavior[PubSub[T]] =
    Behaviors.setup[PubSub[T]](ctx ⇒ new PubSubBehavior(ctx, loggerFactory))
}
