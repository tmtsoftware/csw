package csw.services.config.api.internal

import java.nio.file.{Path, Paths}
import java.time.Instant

import csw.services.config.api.models.{ConfigFileInfo, ConfigFileRevision, ConfigId, ConfigMetadata}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json._

/**
 * Convert types to JSON and vice versa
 */
private[config] trait JsonSupport extends PlayJsonSupport {

  implicit val fileFormat: Format[Path] = new Format[Path] {
    override def writes(obj: Path): JsValue = JsString(obj.toString)

    override def reads(json: JsValue): JsResult[Path] = json match {
      case JsString(value) ⇒ JsSuccess(Paths.get(value))
      case _               ⇒ throw new RuntimeException("can not parse")
    }
  }

  implicit val dateFormat: Format[Instant] = new Format[Instant] {
    override def writes(obj: Instant): JsValue = JsString(obj.toString)

    override def reads(json: JsValue): JsResult[Instant] = json match {
      case JsString(value) ⇒ JsSuccess(Instant.parse(value))
      case _               ⇒ throw new RuntimeException("can not parse")
    }
  }

  implicit val configIdFormat: OFormat[ConfigId]                    = Json.format[ConfigId]
  implicit val configFileInfoFormat: OFormat[ConfigFileInfo]        = Json.format[ConfigFileInfo]
  implicit val configFileHistoryFormat: OFormat[ConfigFileRevision] = Json.format[ConfigFileRevision]
  implicit val configMetadataFormat: OFormat[ConfigMetadata]        = Json.format[ConfigMetadata]
}
