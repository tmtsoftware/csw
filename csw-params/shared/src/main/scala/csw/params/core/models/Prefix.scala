package csw.params.core.models

import csw.params.core.models.Prefix.SEPARATOR

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
}
