package csw.testkit.scaladsl

sealed trait CSWService extends Product with Serializable

/**
 * Supported services by framework testkit.
 *
 * Specify one or more services from following ADT's while creating FrameworkTestKit
 * and testkit will make sure that those services are started.
 */
object CSWService {
  case object LocationServer extends CSWService
  case object ConfigServer   extends CSWService
  case object EventStore     extends CSWService
  case object AlarmStore     extends CSWService
}
