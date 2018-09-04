package csw.messages.params.models

import java.util.Optional

import csw.messages.TMTSerializable
import play.api.libs.json._

object ObsId {

  implicit val format: Format[ObsId] = new Format[ObsId] {
    override def writes(obj: ObsId): JsValue           = JsString(obj.obsId)
    override def reads(json: JsValue): JsResult[ObsId] = JsSuccess(ObsId(json.as[String]))
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
