package csw.framework.internal.configparser

import com.typesafe.config.{Config, ConfigRenderOptions}
import csw.framework.models.{ContainerInfo, HostBootstrapInfo}
import csw.messages.framework.ComponentInfo
import play.api.libs.json._

object ConfigParser {
  def parseContainer(config: Config): ContainerInfo  = parse[ContainerInfo](config)
  def parseStandalone(config: Config): ComponentInfo = parse[ComponentInfo](config)
  def parseHost(config: Config): HostBootstrapInfo   = parse[HostBootstrapInfo](config)

  private def parse[T: Format](config: Config): T = {
    val json = configToJsValue(config)
    Json.fromJson[T](json) match {
      case JsSuccess(value, path) ⇒ value
      case err @ JsError(errors) ⇒
        throw new RuntimeException(massageErrors(err))
    }
  }

  private def configToJsValue(config: Config): JsValue = Json.parse(config.root().render(ConfigRenderOptions.concise()))

  private def massageErrors(jsError: JsError): String =
    jsError.errors
      .map {
        case (path, errors) ⇒
          val str = errors.map(_.message).mkString(",")
          s"error at $path -> $str"
      }
      .mkString("\n")
}
