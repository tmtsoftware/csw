package csw.framework.models

import play.api.libs.json.{Json, OFormat}

/**
 * @param containerCmdAppExecPath   generated shell script path of container command app from task `universal:publish` (sbt-native-packager task)
 * @param mode                      mode in which container needs to be started. Ex. Standalone or Container
 * @param configFilePath            path of configuration file which is provided to container cmd app to start specified components from config
 * @param configFileLocation        indicator to fetch config file either from local machine or from Configuration service
 */
case class ContainerBootstrapInfo(
    containerCmdAppExecPath: String,
    mode: ContainerMode,
    configFilePath: String,
    configFileLocation: ConfigFileLocation
)

case object ContainerBootstrapInfo {
  implicit val format: OFormat[ContainerBootstrapInfo] = Json.format[ContainerBootstrapInfo]
}
