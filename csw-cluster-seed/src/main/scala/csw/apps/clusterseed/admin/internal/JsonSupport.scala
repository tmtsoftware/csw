package csw.apps.clusterseed.admin.internal

import csw.apps.clusterseed.commons.ClusterSeedLogger
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.models.LogMetadata
import csw.services.logging.scaladsl.Logger
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json._

trait JsonSupport extends PlayJsonSupport {
  val log: Logger = ClusterSeedLogger.getLogger

  implicit val levelFormat: Format[Level] = new Format[Level] {
    override def writes(obj: Level): JsValue = JsString(obj.name)

    override def reads(json: JsValue): JsResult[Level] = json match {
      case JsString(value) ⇒ JsSuccess(Level(value))
      case _ ⇒
        val runtimeException = new RuntimeException(s"can not parse $json")
        log.error(runtimeException.getMessage, ex = runtimeException)
        throw runtimeException
    }
  }

  implicit val logMetadataFormat: Format[LogMetadata] = Json.format[LogMetadata]
}
