package csw.services.event.internal.redis

import csw.messages.events.Event

sealed trait RedisMessage
object RedisMessage {
  case class Set(event: Event) extends RedisMessage
}
