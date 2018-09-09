package csw.messages.location

import csw.messages.TMTSerializable
import csw.messages.location.ConnectionType.{AkkaType, HttpType, TcpType}
import play.api.libs.json._

/**
 * Represents a connection based on a componentId and the type of connection offered by the component
 *
 * @param connectionType represents a type of connection offered by the Component
 */
sealed abstract class Connection(val connectionType: ConnectionType) extends TMTSerializable {
  self: TypedConnection[_] ⇒

  /**
   * A covariant Location type
   */
  type L <: Location

  /**
   * The component that is providing this connection
   */
  def componentId: ComponentId

  /**
   * Returns a ConnectionInfo which represents component name, component type and connection type for this Connection
   */
  def connectionInfo: ConnectionInfo = ConnectionInfo(componentId.name, componentId.componentType, connectionType)

  /**
   * Creates a unique name for Connection based on Component name, ComponentType and ConnectionType
   */
  def name: String = connectionInfo.toString

  /**
   * A helper method to cast this Connection to TypedConnection
   *
   * @tparam T A covariant of Location type that TypedConnection uses
   * @return A TypedConnection casted from this Connection
   */
  def of[T <: Location]: TypedConnection[T] = self.asInstanceOf[TypedConnection[T]]
}

/**
 * TypedConnection captures the type of Location that concrete connection will resolve to
 *
 * @param connectionType represents the type of connection e.g akka, http, tcp
 * @tparam T represents the type of Location
 */
abstract sealed class TypedConnection[T <: Location](connectionType: ConnectionType) extends Connection(connectionType) {
  override type L = T
}

object Connection {

  /**
   * Create a Connection from provided String input
   *
   * @param input is the string representation of connection e.g. TromboneAssembly-assembly-akka
   * @return a Connection model created from string
   */
  def from(input: String): Connection = input.split("-") match {
    case Array(name, componentType, connectionType) ⇒
      from(ConnectionInfo(name, ComponentType.withName(componentType), ConnectionType.withName(connectionType)))
    case _ ⇒ throw new IllegalArgumentException(s"Unable to parse '$input' to make Connection object")
  }

  /**
   * Create a Connection from provided ConnectionInfo
   *
   * @param connectionInfo represents component name, component type and connection type
   * @return A Connection created from connectionInfo
   */
  def from(connectionInfo: ConnectionInfo): Connection = from(
    ComponentId(connectionInfo.name, connectionInfo.componentType),
    connectionInfo.connectionType
  )

  private def from(componentId: ComponentId, connectionType: ConnectionType): Connection = connectionType match {
    case AkkaType ⇒ AkkaConnection(componentId)
    case TcpType  ⇒ TcpConnection(componentId)
    case HttpType ⇒ HttpConnection(componentId)
  }

  implicit val connectionReads: Reads[Connection]   = ConnectionInfo.connectionInfoFormat.map(Connection.from)
  implicit val connectionWrites: Writes[Connection] = Writes[Connection](c ⇒ Json.toJson(c.connectionInfo))

  /**
   * Represents a connection offered by remote Actors e.g. TromboneAssembly-assembly-akka or TromboneHcd-hcd-akka
   */
  case class AkkaConnection(componentId: ComponentId) extends TypedConnection[AkkaLocation](AkkaType)

  /**
   * Represents a http connection provided by the component e.g. ConfigServer-service-http
   */
  case class HttpConnection(componentId: ComponentId) extends TypedConnection[HttpLocation](HttpType)

  /**
   * represents a tcp connection provided by the component e.g. EventService-service-tcp
   */
  case class TcpConnection(componentId: ComponentId) extends TypedConnection[TcpLocation](TcpType)
}
