package csw.aas.core.commons
import csw.location.models.ComponentId
import csw.location.models.Connection.HttpConnection
import csw.location.models.ComponentType

/**
 * `AASConnection` is a wrapper over predefined `HttpConnection` representing Authentication and Authorization service.
 * It is used to register with location service.
 */
object AASConnection {
  val value = HttpConnection(ComponentId("AAS", ComponentType.Service))
}
