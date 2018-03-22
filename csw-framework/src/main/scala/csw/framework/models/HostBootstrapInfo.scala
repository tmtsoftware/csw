package csw.framework.models

import play.api.libs.json.{Json, OFormat}

private[framework] case class HostBootstrapInfo(containers: Set[ContainerBootstrapInfo])

case object HostBootstrapInfo {
  private[csw] implicit val format: OFormat[HostBootstrapInfo] = Json.format[HostBootstrapInfo]
}
