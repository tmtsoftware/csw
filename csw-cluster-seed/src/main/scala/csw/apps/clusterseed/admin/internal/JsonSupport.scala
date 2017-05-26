package csw.apps.clusterseed.admin.internal

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import csw.services.logging.internal.LoggingLevels.Level
import spray.json.{DefaultJsonProtocol, JsObject, JsString, JsValue, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val levelFormat: RootJsonFormat[Level] = new RootJsonFormat[Level] {
    override def write(obj: Level): JsValue = JsObject("level" → JsString(obj.name))

    override def read(json: JsValue): Level = json match {
      case JsString(value) ⇒ Level(value)
      case _               ⇒ throw new RuntimeException("can not parse")
    }
  }
}
