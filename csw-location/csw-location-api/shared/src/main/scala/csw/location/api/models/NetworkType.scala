package csw.location.api.models

sealed abstract class NetworkType(val envKey: String)

object NetworkType {
  case object Public  extends NetworkType("PUBLIC_INTERFACE_NAME")
  case object Private extends NetworkType("INTERFACE_NAME")
}
