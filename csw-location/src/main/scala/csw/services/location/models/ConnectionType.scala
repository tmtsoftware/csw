package csw.services.location.models

import enumeratum._

import scala.collection.immutable.IndexedSeq

/**
  * Represents a type of connection offered by the `Component`
  *
  * @param entryName A name of the connection e.g. akka, http or tcp
  */
sealed abstract class ConnectionType(override val entryName: String) extends EnumEntry with TmtSerializable {
  def name: String = entryName
}

object ConnectionType extends Enum[ConnectionType] {

  /**
    * Return all `ConnectionType` values
    */
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
