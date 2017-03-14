package csw.services.location.scaladsl.models

import enumeratum._

import scala.collection.immutable.IndexedSeq

/**
 * Connection type: Indicate if it is an http server or an akka actor.
 */
sealed abstract class ConnectionType(override val entryName: String) extends EnumEntry {
  def name: String = entryName
}

object ConnectionType extends Enum[ConnectionType] {

  override def values: IndexedSeq[ConnectionType] = findValues

  /**
   * Type of a REST/HTTP based service
   */
  case object HttpType extends ConnectionType("http")

  /**
   * Type of a TCP based service
   */
  case object TcpType extends ConnectionType("tcp")

  /**
   * Type of an Akka actor based service
   */
  case object AkkaType extends ConnectionType("akka")
}
