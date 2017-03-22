package csw.services.location.models

import csw.services.location.models.ConnectionType.{AkkaType, HttpType, TcpType}

import scala.util.{Failure, Try}
import scala.util.control.NonFatal

/**
  * Describes a component and the way it is accessed (http, akka)
  */
sealed abstract class Connection extends Serializable {
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
  def parse(s: String): Try[Connection] = tryParse(s) recoverWith {
    case NonFatal(ex) => Failure(new InvalidConnectionStringException(ex.getMessage))
  }

  private def tryParse(s: String): Try[Connection] = Try {
    s.split("-") match {
      case Array(name, compType, connType) =>
        val componentId = ComponentId(name, ComponentType.withName(compType))
        val connectionType = ConnectionType.withName(connType)
        apply(componentId, connectionType)
      case _ => throw new RuntimeException("connection string should have exactly 3 parts separated by '-', e.g, compName-hcd-akka")
    }
  }

  /**
    * Gets a Connection based on the component id and connection type
    */
  def apply(componentId: ComponentId, connectionType: ConnectionType): Connection = connectionType match {
    case AkkaType => AkkaConnection(componentId)
    case HttpType => HttpConnection(componentId)
    case TcpType  => TcpConnection(componentId)
  }

  /**
    * Exception throws for an invalid connection strings
    */
  class InvalidConnectionStringException(message: String) extends Exception(message)

}
