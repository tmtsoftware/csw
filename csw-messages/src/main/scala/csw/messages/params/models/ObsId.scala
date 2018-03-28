package csw.messages.params.models

import java.util.Optional

import scalapb.TypeMapper
import csw.messages.TMTSerializable
import play.api.libs.json._

import scala.language.implicitConversions
import scala.util.matching.Regex

object ObsId {

  private[messages] implicit val format: Format[ObsId] = new Format[ObsId] {
    override def writes(obj: ObsId): JsValue           = JsString(obj.obsId)
    override def reads(json: JsValue): JsResult[ObsId] = JsSuccess(ObsId(json.as[String]))
  }

  implicit val mapper: TypeMapper[String, Option[ObsId]] = TypeMapper[String, Option[ObsId]] { x ⇒
    if (x.isEmpty) None else Some(ObsId(x))
  } { x ⇒
    x.getOrElse(empty).obsId
  }

  /**
   * Represents an empty ObsId
   *
   * @return an ObsId with empty string
   */
  def empty: ObsId = ObsId("")
}

/**
 * Represents a unique observation id
 *
 * @param obsId the string representation of obsId
 */
case class ObsId(obsId: String) extends TMTSerializable {

  /**
   * Returns the ObsId in form of Option
   *
   * @return a defined Option with obsId
   */
  def asOption: Option[ObsId] = Some(new ObsId(obsId))

  /**
   * Returns the ObsId in form of Optional
   *
   * @return a defined Optional with obsId
   */
  def asOptional: Optional[ObsId] = Optional.of(new ObsId(obsId))
}
