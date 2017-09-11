package csw.common.framework.models

import play.api.libs.json._

final case class ContainerInfo(
    name: String,
    locationServiceUsage: LocationServiceUsage,
    components: Set[ComponentInfo]
) {
  require(!components.isEmpty, "components can not be empty.")
}

case object ContainerInfo {
  implicit val format: OFormat[ContainerInfo] = Json.format[ContainerInfo]
}
