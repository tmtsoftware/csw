package csw.framework.models

import play.api.libs.json.{Json, OFormat}

case class ContainerBootstrapInfo(
    containerCmdApp: String,
    mode: ContainerMode,
    configFilePath: String,
    configFileLocation: ConfigFileLocation
)

case object ContainerBootstrapInfo {
  implicit val format: OFormat[ContainerBootstrapInfo] = Json.format[ContainerBootstrapInfo]
}
