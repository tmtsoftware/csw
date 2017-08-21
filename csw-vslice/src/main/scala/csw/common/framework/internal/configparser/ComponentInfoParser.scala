package csw.common.framework.internal.configparser

import com.typesafe.config.{Config, ConfigRenderOptions}
import csw.common.framework.models.{ComponentInfo, ContainerInfo}
import csw.services.logging.scaladsl.GenericLogger
import spray.json._

object ComponentInfoParser extends GenericLogger.Simple {
  def parse(config: Config): ContainerInfo           = configToJsValue(config).convertTo[ContainerInfo]
  def parseStandalone(config: Config): ComponentInfo = configToJsValue(config).convertTo[ComponentInfo]

  private def configToJsValue(config: Config): JsValue = config.root().render(ConfigRenderOptions.concise()).parseJson
}
