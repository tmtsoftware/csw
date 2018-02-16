package csw.services.event.internal.api

import io.lettuce.core.pubsub.api.reactive.ChannelMessage

case class EventMessage[K, V](key: K, value: V)

object EventMessage {
  def from[K, V](msg: ChannelMessage[K, V]): EventMessage[K, V] = EventMessage(msg.getChannel, msg.getMessage)
}
