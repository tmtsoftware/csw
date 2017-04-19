package csw.services.location.models

import csw.services.location.models.ConnectionType.{AkkaType, HttpType, TcpType}

/**
 * Represents a connection based on a componentId and the type of connection offered by the component
 */
sealed abstract class Connection(val connectionType: ConnectionType) extends TmtSerializable {

  /**
   * The component that is providing this connection
   */
  def componentId: ComponentId

  /**
   * Creates a unique name for Connection based on Component name, ComponentType and ConnectionType
   */
  def name: String = s"${componentId.name}-${componentId.componentType.name}-${connectionType.name}"
}

object Connection {

  /**
   * Represents a connection offered by remote Actors
   */
  final case class AkkaConnection(componentId: ComponentId) extends Connection(AkkaType)

  /**
   * Represents a http connection provided by the component
   */
  final case class HttpConnection(componentId: ComponentId) extends Connection(HttpType)

  /**
   * represents a tcp connection provided by the component
   */
  final case class TcpConnection(componentId: ComponentId) extends Connection(TcpType)

}
