package csw.services.location.models

import csw.services.location.models.ConnectionType.{AkkaType, HttpType, TcpType}

/**
  * Describes a component and the way it is accessed (http, akka)
  * connectionType: Indicates how the component is accessed (http, akka)
  */
sealed abstract class Connection(val connectionType: ConnectionType) extends TmtSerializable {
  /**
    * Holds the component name and type (assembly, hcd, etc.)
    */
  def componentId: ComponentId

  def name: String = s"${componentId.name}-${componentId.componentType.name}-${connectionType.name}"
}

object Connection {

  /**
    * A connection to a remote akka actor based component
    */
  final case class AkkaConnection(componentId: ComponentId) extends Connection(AkkaType)

  /**
    * A connection to a remote http based component
    */
  final case class HttpConnection(componentId: ComponentId) extends Connection(HttpType)

  /**
    * A connection to a remote tcp based component
    */
  final case class TcpConnection(componentId: ComponentId) extends Connection(TcpType)

}
