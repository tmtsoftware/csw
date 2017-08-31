package csw.services.location.models

import acyclic.skipped
import csw.services.location.internal.ConnectionInfo
import csw.services.location.models.ConnectionType.{AkkaType, HttpType, TcpType}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

/**
 * Represents a connection based on a componentId and the type of connection offered by the component
 */
sealed abstract class Connection(val connectionType: ConnectionType) extends TmtSerializable {

  type L <: Location

  /**
   * The component that is providing this connection
   */
  def componentId: ComponentId

  def connectionInfo: ConnectionInfo =
    ConnectionInfo(componentId.name, componentId.componentType.name, connectionType.name)

  /**
   * Creates a unique name for Connection based on Component name, ComponentType and ConnectionType
   */
  def name: String = connectionInfo.toString
}

abstract sealed class TypedConnection[T <: Location](connectionType: ConnectionType)
    extends Connection(connectionType) {
  override type L = T
}

object Connection {

  def from(input: String): Connection = input.split("-") match {
    case Array(name, componentType, connectionType) ⇒ from(ConnectionInfo(name, componentType, connectionType))
    case _                                          ⇒ throw new IllegalArgumentException(s"Unable to parse '$input' to make Connection object")
  }

  def from(connectionInfo: ConnectionInfo): Connection = from(
    ComponentId(connectionInfo.name, ComponentType.withName(connectionInfo.componentType)),
    ConnectionType.withName(connectionInfo.connectionType)
  )

  private def from(componentId: ComponentId, connectionType: ConnectionType): Connection = connectionType match {
    case AkkaType ⇒ AkkaConnection(componentId)
    case TcpType  ⇒ TcpConnection(componentId)
    case HttpType ⇒ HttpConnection(componentId)
  }

  implicit val connectionReads: Reads[Connection] = ({
    (JsPath \ "name").read[String] and
    (JsPath \ "componentType").read[String] and
    (JsPath \ "connectionType").read[String]
  })((a, b, c) ⇒ ConnectionInfo.apply(a, b, c)).map(info ⇒ Connection.from(info))

  implicit val connectionWrites: Writes[Connection] = Writes(
    c ⇒ Json.obj("name" → c.name, "componentType" → c.componentId.componentType, "connectionType" → c.connectionType)
  )

  /**
   * Represents a connection offered by remote Actors
   */
  case class AkkaConnection(componentId: ComponentId) extends TypedConnection[AkkaLocation](AkkaType)

  /**
   * Represents a http connection provided by the component
   */
  case class HttpConnection(componentId: ComponentId) extends TypedConnection[HttpLocation](HttpType)

  /**
   * represents a tcp connection provided by the component
   */
  case class TcpConnection(componentId: ComponentId) extends TypedConnection[TcpLocation](TcpType)
}
