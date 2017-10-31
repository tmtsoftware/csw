package csw.framework.models

import play.api.libs.json.{Json, OFormat}

case class HostBootstrapInfo(containers: Set[ContainerBootstrapInfo])

case object HostBootstrapInfo {
  implicit val format: OFormat[HostBootstrapInfo] = Json.format[HostBootstrapInfo]
}
