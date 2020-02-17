package csw.aas.core.commons
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.models.Connection.HttpConnection
import csw.prefix.models.{Prefix, Subsystem}

/**
 * `AASConnection` is a wrapper over predefined `HttpConnection` representing Authentication and Authorization service.
 * It is used to register with location service.
 */
object AASConnection {
  val value = HttpConnection(ComponentId(Prefix(Subsystem.CSW, "AAS"), ComponentType.Service))
}
