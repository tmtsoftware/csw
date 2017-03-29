package csw.services.location.models

import csw.services.location.models.ConnectionType.{AkkaType, HttpType, TcpType}

/**
  * Manages a unique connection based on `ConnectionType` and [[csw.services.location.models.ComponentId]]
  *
  * @param connectionType A type of connection offered by component e.g. akka, http or tcp
  */
sealed abstract class Connection(val connectionType: ConnectionType) extends TmtSerializable {
  /**
    * A unique component id based on `Component name` and [[csw.services.location.models.ComponentType]]
    */
  def componentId: ComponentId

  /**
    * Creates a unique name for `Connection` composed of `Component name`, `ComponentType` and `ConnectionType`
    */
  def name: String = s"${componentId.name}-${componentId.componentType.name}-${connectionType.name}"
}

object Connection {

  /**
    * A connection to a remote akka based component
    *
    * @param componentId A component providing akka services
    */
  final case class AkkaConnection(componentId: ComponentId) extends Connection(AkkaType)

  /**
    * A connection to a remote http based component
    *
    * @param componentId A component providing http services
    */
  final case class HttpConnection(componentId: ComponentId) extends Connection(HttpType)

  /**
    * A connection to a remote tcp based component
    *
    * @param componentId A component providing tcp services
    */
  final case class TcpConnection(componentId: ComponentId) extends Connection(TcpType)

}
