package csw.framework.internal.pubsub

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.params.commands.Nameable
import csw.command.client.models.framework.PubSub
import csw.logging.client.scaladsl.LoggerFactory

/**
 * Factory for creating [[akka.actor.typed.scaladsl.AbstractBehavior]] of a pub sub actor
 */
private[framework] class PubSubBehaviorFactory() {
  def make[T: Nameable](loggerFactory: LoggerFactory): Behavior[PubSub[T]] =
    PubSubBehavior.behavior(loggerFactory)

}
