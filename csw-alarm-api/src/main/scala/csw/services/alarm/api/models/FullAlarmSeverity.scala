package csw.services.alarm.api.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
 * Represents all the severities an alarm can have including Disconnected. The complete list of severities is Okay, Warning,
 * Major, Indeterminate, Disconnected, Critical.
 *
 * @param level is fundamental in comparing severities
 */
sealed abstract class FullAlarmSeverity private[alarm] (val level: Int) extends EnumEntry with Lowercase {

  /**
   * The name of SeverityLevels e.g. for Major severity level, the name will be represented as `major`
   */
  def name: String = entryName

  private[alarm] def >(otherSeverity: FullAlarmSeverity): Boolean = this.level > otherSeverity.level

  private[alarm] def max(otherSeverity: FullAlarmSeverity): FullAlarmSeverity = if (otherSeverity > this) otherSeverity else this
}

object FullAlarmSeverity extends Enum[FullAlarmSeverity] {

  /**
   * Returns a sequence of all alarm severity
   */
  def values: IndexedSeq[FullAlarmSeverity] = findValues ++ AlarmSeverity.values

  case object Disconnected extends FullAlarmSeverity(4)
}

/**
 * Represents the alarm severity set by the component developer e.g Okay, Warning, Major, Indeterminate, Critical.
 * Disconnected is not a part of AlarmSeverity as it is never set by the component developer.
 *
 * @param level is fundamental in comparing severities
 */
sealed abstract class AlarmSeverity private[alarm] (override val level: Int) extends FullAlarmSeverity(level)

object AlarmSeverity extends Enum[AlarmSeverity] {

  /**
   * Returns a sequence of all alarm severity (except Disconnected)
   */
  def values: IndexedSeq[AlarmSeverity] = findValues

  case object Okay          extends AlarmSeverity(0)
  case object Warning       extends AlarmSeverity(1)
  case object Major         extends AlarmSeverity(2)
  case object Indeterminate extends AlarmSeverity(3)
  case object Critical      extends AlarmSeverity(5)
}
