package romaine.codec

import java.nio.ByteBuffer

import io.lettuce.core.codec.RedisCodec
import romaine.codec.RomaineByteCodec.{FromBytes, ToBytes}

class RomaineRedisCodec[K: RomaineByteCodec, V: RomaineByteCodec] extends RedisCodec[K, V] {
  override def decodeKey(bytes: ByteBuffer): K   = bytes.as[K]
  override def decodeValue(bytes: ByteBuffer): V = bytes.as[V]

  override def encodeKey(key: K): ByteBuffer     = key.asBytes
  override def encodeValue(value: V): ByteBuffer = value.asBytes
}
