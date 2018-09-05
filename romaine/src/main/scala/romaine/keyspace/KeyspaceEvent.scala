package romaine.keyspace

sealed trait KeyspaceEvent[+T]

object KeyspaceEvent {
  final case class Updated[T](value: T) extends KeyspaceEvent[T]
  final case object Removed             extends KeyspaceEvent[Nothing]
  final case class Error(msg: String)   extends KeyspaceEvent[Nothing]
}
