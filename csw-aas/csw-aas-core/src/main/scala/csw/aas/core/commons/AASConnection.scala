package csw.aas.core.commons
import csw.location.models.{ComponentId, ComponentType}
import csw.location.models.Connection.HttpConnection
import csw.prefix.{Prefix, Subsystem}

/**
 * `AASConnection` is a wrapper over predefined `HttpConnection` representing Authentication and Authorization service.
 * It is used to register with location service.
 */
object AASConnection {
  val value = HttpConnection(ComponentId(Prefix(Subsystem.CSW, "AAS"), ComponentType.Service))
}
