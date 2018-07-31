package romaine

case class RedisResult[K, V](key: K, value: V)
