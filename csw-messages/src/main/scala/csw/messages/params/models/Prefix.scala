package csw.messages.params.models

import csw.messages.TMTSerializable
import csw.messages.params.models.Prefix.SEPARATOR
import play.api.libs.json.{Json, OFormat}

/**
 * A top level key for a parameter set: combination of subsystem and the subsystem's prefix
 * Eg. tcs.filter.wheel
 *
 * @param prefix    the subsystem's prefix
 */
case class Prefix(prefix: String) extends TMTSerializable {
  val subsystem: Subsystem = {
    require(prefix != null)
    Subsystem.withNameOption(prefix.splitAt(prefix.indexOf(SEPARATOR))._1).getOrElse(Subsystem.BAD)
  }
}

object Prefix {
  private val SEPARATOR = "."

  private[messages] implicit val format: OFormat[Prefix] = Json.format[Prefix]
}
