package csw.params.core.models

/**
 * A top level key for a parameter set: combination of subsystem and the subsystem's prefix
 * Eg. tcs.filter.wheel
 *
 * @note Component name should not contain
 *  - leading or trailing spaces
 *  - and hyphen (-)
 * @param subsystem     component subsystem
 * @param componentName component name
 */
case class Prefix(subsystem: Subsystem, componentName: String) {
  require(componentName == componentName.trim, "component name has leading and trailing whitespaces")

  require(!componentName.contains("-"), "component name has '-'")

  val key = s"${subsystem.name}${Prefix.SEPARATOR}$componentName"

  override def toString: String = key
}

object Prefix {
  def apply(key: String): Prefix = {
    require(key.contains(SEPARATOR))
    val parts = key.splitAt(key.indexOf(SEPARATOR))
    Prefix(Subsystem.withNameInsensitive(parts._1), parts._2.tail)
  }
  private val SEPARATOR = "."
}
