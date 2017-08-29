package csw.services.location.models

import csw.services.location.models.ConnectionType.{AkkaType, HttpType, TcpType}
import spray.json.{JsString, JsValue, JsonFormat}

/**
 * Represents a connection based on a componentId and the type of connection offered by the component
 */
sealed abstract class Connection(val connectionType: ConnectionType) extends TmtSerializable {

  type L <: Location

  /**
   * The component that is providing this connection
   */
  def componentId: ComponentId

  /**
   * Creates a unique name for Connection based on Component name, ComponentType and ConnectionType
   */
  def name: String = s"${componentId.name}-${componentId.componentType.name}-${connectionType.name}"
}

abstract class TypedConnection[T <: Location](connectionType: ConnectionType) extends Connection(connectionType) {
  override type L = T
}

object Connection {

  import csw.services.location.internal.JsonSupport._

  def from(input: String): Connection =
    input.split("-") match {
      case Array(component, componentType, AkkaType.entryName) ⇒
        AkkaConnection(ComponentId(component, ComponentType.withName(componentType)))
      case Array(component, componentType, TcpType.entryName) ⇒
        TcpConnection(ComponentId(component, ComponentType.withName(componentType)))
      case Array(component, componentType, HttpType.entryName) ⇒
        HttpConnection(ComponentId(component, ComponentType.withName(componentType)))
      case _ ⇒ throw new IllegalArgumentException(s"Unable to parse '$input' to make Connection object")
    }

  implicit val format: JsonFormat[Connection] = new JsonFormat[Connection] {
    override def read(json: JsValue): Connection = Connection.from(json.convertTo[String])
    override def write(obj: Connection): JsValue = JsString(obj.name)
  }

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
