package romaine.codec
import java.nio.ByteBuffer

import io.lettuce.core.codec.RedisCodec

class RomaineRedisCodec[K: RomaineByteCodec, V: RomaineByteCodec] extends RedisCodec[K, V] {
  override def decodeKey(bytes: ByteBuffer): K   = RomaineByteCodec[K].fromBytes(bytes)
  override def decodeValue(bytes: ByteBuffer): V = RomaineByteCodec[V].fromBytes(bytes)

  override def encodeKey(key: K): ByteBuffer     = RomaineByteCodec[K].toBytes(key)
  override def encodeValue(value: V): ByteBuffer = RomaineByteCodec[V].toBytes(value)
}
