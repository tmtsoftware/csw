package csw.testkit.scaladsl

sealed trait CSWService extends Product with Serializable

object CSWService {
  case object LocationServer extends CSWService
  case object ConfigServer   extends CSWService
  case object EventStore     extends CSWService
  case object AlarmStore     extends CSWService
}
