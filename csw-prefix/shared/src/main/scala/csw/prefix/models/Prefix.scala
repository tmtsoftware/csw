/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.prefix.models

/**
 * A top level key for a parameter set: combination of subsystem and the subsystem's prefix
 * e.g. tcs.filter.wheel, wfos.prog.cloudcover, etc
 *
 * @note Component name should not contain
 *  - leading or trailing spaces
 *  - and hyphen (-)
 * @param subsystem     component subsystem - tcs (TCS), wfos (WFOS)
 * @param componentName component name - filter.wheel, prog.cloudcover
 */
case class Prefix(subsystem: Subsystem, componentName: String) {
  require(componentName == componentName.trim, "component name has leading and trailing whitespaces")
  require(!componentName.contains("-"), "component name has '-'")

  /**
   * String representation of prefix e.g. tcs.filter.wheel where tcs is the subsystem name and filter.wheel is the component name
   */
  override def toString: String = s"${subsystem.name}${Prefix.SEPARATOR}$componentName"
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
    value.split("\\" + SEPARATOR, 2) match {
      case Array(subsystem, componentName) => Prefix(Subsystem.withNameInsensitive(subsystem), componentName)
      case x                               => throw new MatchError(x)
    }
  }
}
