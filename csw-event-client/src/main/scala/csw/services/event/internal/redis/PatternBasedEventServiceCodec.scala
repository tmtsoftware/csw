package csw.services.event.internal.redis

import java.nio.ByteBuffer

import csw.messages.events.Event
import io.lettuce.core.codec.RedisCodec

/**
 * Encodes and decodes keys as Strings and values as ProtoBuf byte equivalent of Event
 */
//object PatternBasedEventServiceCodec extends RedisCodec[String, Event] with BaseEventServiceCodec {
//  override def decodeKey(bytes: ByteBuffer): String = super.decodeStringKey(bytes)
//  override def encodeKey(key: String): ByteBuffer   = super.encodeStringKey(key)
//}
