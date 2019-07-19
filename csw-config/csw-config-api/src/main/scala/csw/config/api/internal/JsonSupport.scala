package csw.config.api.internal

import java.nio.file.{Path, Paths}
import java.time.Instant

import com.github.ghik.silencer.silent
import csw.config.models.{ConfigFileInfo, ConfigFileRevision, ConfigId, ConfigMetadata}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json._

/**
 * Convert types to JSON and vice versa
 */
private[config] trait JsonSupport extends PlayJsonSupport {

  implicit val fileFormat: Format[Path] = new Format[Path] {
    override def writes(obj: Path): JsValue = JsString(obj.toString)

    override def reads(json: JsValue): JsResult[Path] = json match {
      case JsString(value) => JsSuccess(Paths.get(value))
      case _               => JsError(s"can not parse $json into Path")
    }
  }

  implicit val dateFormat: Format[Instant] = new Format[Instant] {
    override def writes(obj: Instant): JsValue = JsString(obj.toString)

    override def reads(json: JsValue): JsResult[Instant] = json match {
      case JsString(value) => JsSuccess(Instant.parse(value))
      case _               => throw new RuntimeException("can not parse")
    }
  }

  implicit val configIdFormat: Format[ConfigId] = Json.format[ConfigId]

  implicit val configFileInfoFormat: OFormat[ConfigFileInfo] = {

    // The warnings on this variable are silenced because to avoid "local val configIdFormat is never used"
    // It shows this warning because this variable is not directly being used but is consumed by a macro instead
    @silent implicit val configIdFormat: Format[ConfigId] = new Format[ConfigId] {
      override def writes(o: ConfigId): JsValue = JsString(o.id)
      override def reads(json: JsValue): JsResult[ConfigId] = json match {
        case JsString(x) => JsSuccess(ConfigId(x))
        case _           => JsError(s"can not parse $json into configId")
      }
    }
    Json.format[ConfigFileInfo]
  }

  implicit val configFileHistoryFormat: OFormat[ConfigFileRevision] = {

    // The warnings on this variable are silenced because to avoid "local val configIdFormat is never used"
    // It shows this warning because this variable is not directly being used but is consumed by a macro instead
    @silent implicit val configIdFormat: Format[ConfigId] = new Format[ConfigId] {
      override def writes(o: ConfigId): JsValue = JsString(o.id)
      override def reads(json: JsValue): JsResult[ConfigId] = json match {
        case JsString(x) => JsSuccess(ConfigId(x))
        case _           => JsError(s"can not parse $json into configId")
      }
    }
    Json.format[ConfigFileRevision]
  }
  implicit val configMetadataFormat: OFormat[ConfigMetadata] = Json.format[ConfigMetadata]
}
