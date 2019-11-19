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

  private val handle            = s"${subsystem.name}${Prefix.SEPARATOR}$componentName"
  val actorSystemName: String   = handle.replace('.', '_')
  override def toString: String = handle
}

object Prefix {
  def apply(handle: String): Prefix = {
    require(handle.contains(SEPARATOR), s"prefix must have a '$SEPARATOR' separator")
    val parts = handle.splitAt(handle.indexOf(SEPARATOR))
    Prefix(Subsystem.withNameInsensitive(parts._1), parts._2.tail)
  }
  private val SEPARATOR = "."
}
