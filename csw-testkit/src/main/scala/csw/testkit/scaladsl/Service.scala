package csw.testkit.scaladsl

sealed trait Service extends Product with Serializable

object Service {
  case object LocationServer extends Service
  case object ConfigServer   extends Service
  case object EventStore     extends Service
  case object AlarmStore     extends Service
}
