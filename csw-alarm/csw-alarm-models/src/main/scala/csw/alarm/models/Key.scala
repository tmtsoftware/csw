/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.models

import java.util.regex.Pattern

import csw.alarm.extension.RichStringExtentions.RichString
import csw.alarm.commons.Separators.KeySeparator
import csw.prefix.models.{Prefix, Subsystem}

/**
 * A wrapper class representing the key for an alarm/component/subsystem/system
 *
 * @note key is case-insensitive e.g nfiraos.trombone.tromboneaxislowlimitalarm and
 * NFIRAOS.trombone.tromboneaxislowlimitalarm keys are equal
 */
sealed abstract class Key private[alarm] (subsystem: String, component: String, name: String) {
  require(component.isDefined, "component should not be an empty value")
  require(name.isDefined, "name should not be an empty value")

  /**
   * Unique value of the key which is combination of subsystem, component and name
   */
  val value: String = s"$subsystem$KeySeparator$component$KeySeparator$name".toLowerCase

  /**
   * Equality of the key is based on the subsystem, component and name. Inlined code from deprecated Proxy class.
   */
  def self: Any              = value
  override def hashCode: Int = self.hashCode
  override def equals(that: Any): Boolean =
    that match {
      case null => false
      case _ =>
        val x = that.asInstanceOf[AnyRef]
        (x eq this.asInstanceOf[AnyRef]) || (x eq self.asInstanceOf[AnyRef]) || (x `equals` self)
    }
  override def toString: String = "" + self
}

object Key {
  // pattern matches for any one of *, [, ], ^, - characters  present
  private val invalidChars: Pattern = Pattern.compile(".*[\\*\\[\\]\\^\\?\\-].*")

  /**
   * Represents unique alarm in the given subsystem and component e.g. nfiraos.trombone.tromboneaxislowlimitalarm
   *
   * @note component and name cannot contain invalid characters i.e. `* [ ] ^ -`
   * @param prefix this alarm belongs to e.g. tcs.filter.wheel
   * @param name represents the name of the alarm unique to the component e.g tromboneAxisLowLimitAlarm
   */
  case class AlarmKey(prefix: Prefix, name: String) extends Key(prefix.subsystem.name, prefix.componentName, name) {
    require(!prefix.componentName.matches(invalidChars), "key contains invalid characters")
    require(!name.matches(invalidChars), "key contains invalid characters")
  }

  /**
   * Represents a key for all the alarms of a component
   *
   * @note component cannot contain invalid characters i.e. `* [ ] ^ -`
   * @param prefix this alarm belongs to e.g. tcs.filter.wheel
   */
  case class ComponentKey(prefix: Prefix) extends Key(prefix.subsystem.name, prefix.componentName, "*") {
    require(!prefix.componentName.matches(invalidChars), "component name contains invalid characters")
  }

  /**
   * Represents a key for all the alarms of a subsystem
   *
   * @param subsystem represents all alarms belonging to this component e.g. NFIRAOS
   */
  case class SubsystemKey(subsystem: Subsystem) extends Key(subsystem.name, "*", "*")

  /**
   * Represents all the alarms available in the system
   */
  case object GlobalKey extends Key("*", "*", "*")

  object AlarmKey {
    private[alarm] def apply(keyStr: String): AlarmKey =
      keyStr.split(KeySeparator) match {
        case Array(subsystem, component, name) => AlarmKey(Prefix(Subsystem.withNameInsensitive(subsystem), component), name)
        case _ => throw new IllegalArgumentException(s"Unable to parse '$keyStr' to make AlarmKey object")
      }
  }
}
