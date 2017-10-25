package csw.apps.clusterseed.admin.internal

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import csw.apps.clusterseed.admin.http.{ErrorMessage, ErrorResponse}
import csw.apps.clusterseed.commons.ClusterSeedLogger
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.models.LogMetadata
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol with ClusterSeedLogger.Simple {
  implicit val levelFormat: RootJsonFormat[Level] = new RootJsonFormat[Level] {
    override def write(obj: Level): JsValue = JsString(obj.name)

    override def read(json: JsValue): Level = json match {
      case JsString(value) ⇒ Level(value)
      case _ ⇒
        val runtimeException = new RuntimeException(s"can not parse $json")
        log.error(runtimeException.getMessage, ex = runtimeException)
        throw runtimeException
    }
  }

  implicit val logMetadataFormat: RootJsonFormat[LogMetadata]     = jsonFormat4(LogMetadata.apply)
  implicit val errorMessageFormat: RootJsonFormat[ErrorMessage]   = jsonFormat2(ErrorMessage.apply)
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat1(ErrorResponse.apply)
}
