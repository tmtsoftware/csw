package csw.location.api.javadsl

import csw.location.models.ConnectionType

/**
 * Helper class for Java to get the handle of `ConnectionType` which is fundamental to LocationService library
 */
object JConnectionType {

  /**
   * Used to define an Akka connection
   */
  val AkkaType: ConnectionType = ConnectionType.AkkaType

  /**
   * Used to define a TCP connection
   */
  val TcpType: ConnectionType = ConnectionType.TcpType

  /**
   * Used to define a HTTP connection
   */
  val HttpType: ConnectionType = ConnectionType.HttpType
}
