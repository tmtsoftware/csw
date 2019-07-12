package csw.aas.core.commons
import csw.location.model.scaladsl.Connection.HttpConnection
import csw.location.model.scaladsl.{ComponentId, ComponentType}

/**
 * `AASConnection` is a wrapper over predefined `HttpConnection` representing Authentication and Authorization service.
 * It is used to register with location service.
 */
object AASConnection {
  val value = HttpConnection(ComponentId("AAS", ComponentType.Service))
}
