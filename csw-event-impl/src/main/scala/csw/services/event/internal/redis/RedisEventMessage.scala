package csw.services.event.internal.redis

import csw.services.event.scaladsl.EventMessage
import io.lettuce.core.pubsub.api.reactive.ChannelMessage

object RedisEventMessage {
  def from[K, V](msg: ChannelMessage[K, V]): EventMessage[K, V] = EventMessage(msg.getChannel, msg.getMessage)
}
