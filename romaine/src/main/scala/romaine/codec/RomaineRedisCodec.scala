package romaine.codec

import java.nio.ByteBuffer

import io.lettuce.core.codec.RedisCodec
import romaine.codec.RomaineCodec.{FromBytes, ToBytesAndString}

class RomaineRedisCodec[K: RomaineCodec, V: RomaineCodec] extends RedisCodec[K, V] {
  override def decodeKey(bytes: ByteBuffer): K = bytes.as[K]
  override def decodeValue(bytes: ByteBuffer): V = bytes.as[V]

  override def encodeKey(key: K): ByteBuffer = key.asBytes
  override def encodeValue(value: V): ByteBuffer = value.asBytes
}
