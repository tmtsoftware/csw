package romaine.keyspace

sealed abstract class KeyspaceEvent[+T](underlying: RedisKeyspaceEvent)

object KeyspaceEvent {
  case class Updated[T](value: T)                    extends KeyspaceEvent[T](RedisKeyspaceEvent.Set)
  case class Wrapped(underlying: RedisKeyspaceEvent) extends KeyspaceEvent[Nothing](underlying)
  case class Error(msg: String)                      extends KeyspaceEvent[Nothing](RedisKeyspaceEvent.Unknown)
}
