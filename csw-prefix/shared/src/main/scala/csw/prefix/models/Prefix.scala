package csw.prefix.models

/**
 * A top level key for a parameter set: combination of subsystem and the subsystem's prefix
 * e.g. tcs.filter.wheel, wfos.prog.cloudcover, etc
 *
 * @note Component name should not contain
 *  - leading or trailing spaces
 *  - and hyphen (-)
 *  - Prefix is case-insensitive i.e. tcs.filter.wheel equals TCS.Filter.Wheel
 * @param subsystem     component subsystem - TCS, WFOS
 * @param componentName component name - filter.wheel, prog.cloudcover
 */
case class Prefix private (subsystem: Subsystem, componentName: String) {
  require(componentName == componentName.trim, "component name has leading and trailing whitespaces")
  require(!componentName.contains("-"), "component name has '-'")

  /**
   * String representation of prefix e.g. TCS.Filter.Wheel where TCS is the subsystem name and Filter.Wheel is the component name
   */
  override val toString: String = s"${subsystem.name}${Prefix.SEPARATOR}$componentName"

  private[csw] val value: String = toString.toLowerCase

  final override def hashCode(): Int = value.hashCode

  final override def equals(obj: Any): Boolean = obj match {
    case that: Prefix => this.value == that.value
    case _            => false
  }

}

object Prefix {
  private val SEPARATOR = "."

  /**
   * Creates a Prefix based on the given value of format tcs.filter.wheel and splits it to have tcs as `subsystem` and filter.wheel
   * as `componentName`
   *
   * @param value of format tcs.filter.wheel
   * @return a Prefix instance
   */
  def apply(value: String): Prefix = {
    require(value.contains(SEPARATOR), s"prefix must have a '$SEPARATOR' separator")
    val Array(subsystem, componentName) = value.split(s"\\$SEPARATOR", 2)
    Prefix(Subsystem.withNameInsensitive(subsystem), componentName)
  }
}
