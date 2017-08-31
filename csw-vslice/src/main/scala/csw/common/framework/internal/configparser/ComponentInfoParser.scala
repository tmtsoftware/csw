package csw.common.framework.internal.configparser

import com.typesafe.config.{Config, ConfigRenderOptions}
import csw.common.framework.models.{ComponentInfo, ContainerInfo}
import play.api.libs.json._

object ComponentInfoParser {
  def parse(config: Config): ContainerInfo = {
    Json.fromJson[ContainerInfo](configToJsValue(config)) match {
      case JsSuccess(value, path) ⇒ value
      case JsError(errors)        ⇒ throw new RuntimeException(errors.toMap.mkString("\n"))
    }
  }
  def parseStandalone(config: Config): ComponentInfo = {
    Json.fromJson[ComponentInfo](configToJsValue(config)) match {
      case JsSuccess(value, path) ⇒ value
      case JsError(errors)        ⇒ throw new RuntimeException(errors.toMap.mkString("\n"))
    }
  }

  private def configToJsValue(config: Config): JsValue = {
    Json.parse(config.root().render(ConfigRenderOptions.concise()))
  }
}
