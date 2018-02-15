package csw.services.event.scaladsl

case class EventMessage[K, V](key: K, value: V)
