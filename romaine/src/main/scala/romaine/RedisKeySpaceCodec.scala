package romaine

import io.lettuce.core.codec.RedisCodec

trait RedisKeySpaceCodec[K, V] extends RedisCodec[K, V] {
  def toKeyString(key: K): String
  def fromKeyString(keyString: String): K
}
