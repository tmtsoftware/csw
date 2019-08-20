package csw.params.core.models
import csw.params.core.models.Prefix.SEPARATOR
import play.api.libs.json._

/**
 * A top level key for a parameter set: combination of subsystem and the subsystem's prefix
 * Eg. tcs.filter.wheel
 *
 * @param prefix    the subsystem's prefix
 */
case class Prefix(prefix: String) {
  val subsystem: Subsystem = {
    require(prefix != null)
    val subsystemStr = prefix.split(SEPARATOR).head // this is safe and will not throw exception
    Subsystem.withNameInsensitive(subsystemStr) // throw exception if invalid subsystem provided
  }
}

object Prefix {
  private val SEPARATOR = '.'
  implicit val format: Format[Prefix] = new Format[Prefix] {
    override def writes(obj: Prefix): JsValue = JsString(obj.prefix)
    override def reads(json: JsValue): JsResult[Prefix] = JsSuccess(Prefix(json.as[String]))
  }
}
