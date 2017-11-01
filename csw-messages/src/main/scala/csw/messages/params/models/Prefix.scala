package csw.messages.params.models

import play.api.libs.json.{Json, OFormat}

import scala.language.implicitConversions

/**
 * Combines subsystem and the subsystem's prefix
 *
 * @param subsystem the subsystem that is the target of the command
 * @param prefix    the subsystem's prefix
 */
case class Prefix(subsystem: Subsystem, prefix: String) {
  override def toString: String = s"[$subsystem, $prefix]"

  /**
   * Creates a Prefix from the given string
   *
   * @return a Prefix object parsed for the subsystem and prefix
   */
  def this(prefix: String) {
    this(Prefix.subsystem(prefix), prefix)
  }
}

/**
 * A top level key for a parameter set: combines subsystem and the subsystem's prefix
 */
object Prefix {
  private val SEPARATOR = '.'

  implicit def apply(prefix: String): Prefix = Prefix(subsystem(prefix), prefix)

  private def subsystem(keyText: String): Subsystem = {
    assert(keyText != null)
    Subsystem.withNameOption(keyText.splitAt(keyText.indexOf(SEPARATOR))._1).getOrElse(Subsystem.BAD)
  }

  implicit val format: OFormat[Prefix] = Json.format[Prefix]
}
