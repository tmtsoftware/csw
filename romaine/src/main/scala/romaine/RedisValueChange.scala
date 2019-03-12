package romaine

case class RedisValueChange[V](oldValue: V, newValue: V)
