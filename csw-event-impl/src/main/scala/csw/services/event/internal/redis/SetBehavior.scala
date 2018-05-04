package csw.services.event.internal.redis

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.messages.events.{Event, EventKey}
import csw.services.event.internal.redis.RedisMessage.Set
import io.lettuce.core.api.async.RedisAsyncCommands

object SetBehavior {
  def setBehavior(redisCommands: RedisAsyncCommands[EventKey, Event]): Behavior[RedisMessage] =
    Behaviors.receive {
      case (_, Set(event)) ⇒
        redisCommands.set(event.eventKey, event)
        Behaviors.same
      case _ ⇒ Behaviors.stopped
    }
}
