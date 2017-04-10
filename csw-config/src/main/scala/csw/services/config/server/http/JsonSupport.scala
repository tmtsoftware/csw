package csw.services.config.server.http

import java.io.File
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import csw.services.config.api.models.{ConfigFileHistory, ConfigFileInfo, ConfigId}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  protected val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  implicit val fileFormat: JsonFormat[File] = new JsonFormat[File] {
    override def write(obj: File): JsValue = JsString(obj.getPath)

    override def read(json: JsValue): File = json match {
      case JsString(value) ⇒ Paths.get(value).toFile
      case _               ⇒ throw new RuntimeException("can not parse")
    }
  }

  implicit val dateFormat: JsonFormat[Date] = new JsonFormat[Date] {
    override def write(obj: Date): JsValue = JsString(simpleDateFormat.format(obj))

    override def read(json: JsValue): Date = json match {
      case JsString(value) ⇒ simpleDateFormat.parse(value)
      case _               ⇒ throw new RuntimeException("can not parse")
    }
  }

  implicit val configIdFormat: RootJsonFormat[ConfigId] = jsonFormat1(new ConfigId(_))
  implicit val configFileInfoFormat: RootJsonFormat[ConfigFileInfo] = jsonFormat3(ConfigFileInfo.apply)
  implicit val configFileHistoryFormat: RootJsonFormat[ConfigFileHistory] = jsonFormat3(ConfigFileHistory.apply)
}
