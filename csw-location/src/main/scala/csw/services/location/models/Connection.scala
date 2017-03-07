package csw.services.location.models

import csw.services.location.models.ConnectionType.{AkkaType, HttpType, TcpType}

import scala.util.Try

/**
  * Describes a component and the way it is accessed (http, akka)
  */
sealed trait Connection {
  /**
    * Holds the component name and type (assembly, hcd, etc.)
    */
  def componentId: ComponentId

  /**
    * Indicates how the component is accessed (http, akka)
    */
  def connectionType: ConnectionType

  def name: String = s"${componentId.name}-${componentId.componentType.name}-${connectionType.name}"
}

object Connection {

  /**
    * A connection to a remote akka actor based component
    */
  final case class AkkaConnection(componentId: ComponentId) extends Connection {
    val connectionType = AkkaType
  }

  /**
    * A connection to a remote http based component
    */
  final case class HttpConnection(componentId: ComponentId) extends Connection {
    val connectionType = HttpType
  }

  /**
    * A connection to a remote tcp based component
    */
  final case class TcpConnection(componentId: ComponentId) extends Connection {
    val connectionType = TcpType
  }

  /**
    * Gets a Connection from a string as output by toString
    */
  def parse(s: String): Try[Connection] = Try {
    val Array(name, compType, connType) = s.split("-")
    val componentId = ComponentId(name, ComponentType.withName(compType))
    val connectionType = ConnectionType.withName(connType)
    apply(componentId, connectionType)
  }

  /**
    * Gets a Connection based on the component id and connection type
    */
  def apply(componentId: ComponentId, connectionType: ConnectionType): Connection = connectionType match {
    case AkkaType => AkkaConnection(componentId)
    case HttpType => HttpConnection(componentId)
    case TcpType  => TcpConnection(componentId)
  }
}
