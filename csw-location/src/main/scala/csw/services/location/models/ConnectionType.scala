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
    * Used to define a HTTP connection
    */
  case object HttpType extends ConnectionType("http")

  /**
    * Used to define a TCP connection
    */
  case object TcpType extends ConnectionType("tcp")

  /**
    * Used to define an Akka connection
    */
  case object AkkaType extends ConnectionType("akka")

}
