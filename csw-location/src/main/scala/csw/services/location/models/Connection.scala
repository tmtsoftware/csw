package csw.services.location.models

import csw.services.location.models.ConnectionType.{AkkaType, HttpType, TcpType}

import scala.util.{Failure, Success, Try}

/**
 * Describes a component and the way it is accessed (http, akka)
 */
sealed trait Connection {
  /**
   * Holds the component name and type (assembly, hcd, etc.)
   */
  def componentId: ComponentId

  /**
   * Returns a connection's name
   */
  def name = componentId.name

  /**
   * Indicates how the component is accessed (http, akka)
   */
  def connectionType: ConnectionType

  override def toString = s"$componentId-$connectionType"

  override def equals(that: Any) = that match {
    case (that: Connection) => this.toString == that.toString
    case _                  => false
  }
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
  def parse(s: String): Try[Connection] = {
    val (id, typ) = s.splitAt(s.lastIndexOf('-')) // To strings
    ConnectionType.parse(typ.drop(1)) match {
      case Success(AkkaType) => ComponentId.parse(id).map(AkkaConnection)
      case Success(HttpType) => ComponentId.parse(id).map(HttpConnection)
      case Success(TcpType)  => ComponentId.parse(id).map(TcpConnection)
      case Failure(ex)       => Failure(ex)
    }
  }

  /**
   * Gets a Connection based on the component id and connection type
   */
  def apply(componentId: ComponentId, connectionType: ConnectionType): Connection = {
    connectionType match {
      case AkkaType => AkkaConnection(componentId)
      case HttpType => HttpConnection(componentId)
      case TcpType  => TcpConnection(componentId)
    }
  }
}
