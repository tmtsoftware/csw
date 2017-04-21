package csw.services.config.api.models

import java.nio.file.{Path, Paths}
import java.time.Instant

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val fileFormat: JsonFormat[Path] = new JsonFormat[Path] {
    override def write(obj: Path): JsValue = JsString(obj.toString)

    override def read(json: JsValue): Path = json match {
      case JsString(value) ⇒ Paths.get(value)
      case _               ⇒ throw new RuntimeException("can not parse")
    }
  }

  implicit val dateFormat: JsonFormat[Instant] = new JsonFormat[Instant] {
    override def write(obj: Instant): JsValue = JsString(obj.toString)

    override def read(json: JsValue): Instant = json match {
      case JsString(value) ⇒ Instant.parse(value)
      case _               ⇒ throw new RuntimeException("can not parse")
    }
  }

  implicit val configIdFormat: RootJsonFormat[ConfigId]                   = jsonFormat1(new ConfigId(_))
  implicit val configFileInfoFormat: RootJsonFormat[ConfigFileInfo]       = jsonFormat3(ConfigFileInfo.apply)
  implicit val configFileHistoryFormat: RootJsonFormat[ConfigFileHistory] = jsonFormat3(ConfigFileHistory.apply)
}
