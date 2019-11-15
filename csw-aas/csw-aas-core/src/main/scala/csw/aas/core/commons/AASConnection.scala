package csw.aas.core.commons
import csw.location.models.ComponentId
import csw.location.models.Connection.HttpConnection
import csw.location.models.ComponentType
import csw.params.core.models.{Prefix, Subsystem}

/**
 * `AASConnection` is a wrapper over predefined `HttpConnection` representing Authentication and Authorization service.
 * It is used to register with location service.
 */
object AASConnection {
  val value = HttpConnection(ComponentId(Prefix(Subsystem.CSW, "AAS"), ComponentType.Service))
}
