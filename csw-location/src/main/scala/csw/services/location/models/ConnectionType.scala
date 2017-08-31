package csw.services.location.models

import enumeratum._

import scala.collection.immutable.IndexedSeq

/**
 * Represents a type of connection offered by the Component
 *
 * @param entryName A name of the connection type e.g. akka, http or tcp
 */
sealed abstract class ConnectionType(override val entryName: String) extends EnumEntry with TmtSerializable {
  def name: String = entryName
}

object ConnectionType extends Enum[ConnectionType] with PlayJsonEnum[ConnectionType] {

  override def values: IndexedSeq[ConnectionType] = findValues

  /**
   * Represents a HTTP type of connection
   */
  case object HttpType extends ConnectionType("http")

  /**
   * Represents a TCP type of connection
   */
  case object TcpType extends ConnectionType("tcp")

  /**
   * Represents an Akka type of connection
   */
  case object AkkaType extends ConnectionType("akka")

}
