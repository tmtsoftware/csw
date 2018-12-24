package csw.aas.core.commons
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.models.Connection.HttpConnection

/**
 * `AASConnection` is a wrapper over predefined `HttpConnection` representing Authentication and Authorization service.
 * It is used to register with location service.
 */
object AASConnection {
  val value = HttpConnection(ComponentId("AAS", ComponentType.Service))
}
