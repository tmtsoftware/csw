package csw.aas.core.commons
import csw.location.model.ComponentId
import csw.location.model.Connection.HttpConnection
import csw.location.model.ComponentType

/**
 * `AASConnection` is a wrapper over predefined `HttpConnection` representing Authentication and Authorization service.
 * It is used to register with location service.
 */
object AASConnection {
  val value = HttpConnection(ComponentId("AAS", ComponentType.Service))
}
