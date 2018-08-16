package csw.services.alarm.api.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed abstract class AlarmSeverity private[alarm] (val level: Int, val latchable: Boolean) extends EnumEntry with Lowercase {

  /**
   * The name of SeverityLevels e.g. for Major severity level, the name will be represented as `major`
   */
  def name: String = entryName

  def >(otherSeverity: AlarmSeverity): Boolean = this.level > otherSeverity.level

  def max(otherSeverity: AlarmSeverity): AlarmSeverity = if (otherSeverity > this) otherSeverity else this

  // Disconnected, Indeterminate and Okay are not considered as an alarm condition
  def isHighRisk: Boolean = this.level > 0
}

object AlarmSeverity extends Enum[AlarmSeverity] {

  /**
   * Returns a sequence of all alarm severity
   */
  def values: IndexedSeq[AlarmSeverity] = findValues ++ ExplicitAlarmSeverity.values

  case object Disconnected extends AlarmSeverity(4, false)
}

sealed abstract class ExplicitAlarmSeverity private[alarm] (override val level: Int) extends AlarmSeverity(level, true)

object ExplicitAlarmSeverity extends Enum[ExplicitAlarmSeverity] {

  /**
   * Returns a sequence of all alarm severity
   */
  def values: IndexedSeq[ExplicitAlarmSeverity] = findValues

  case object Okay          extends ExplicitAlarmSeverity(0)
  case object Warning       extends ExplicitAlarmSeverity(1)
  case object Major         extends ExplicitAlarmSeverity(2)
  case object Indeterminate extends ExplicitAlarmSeverity(3)
  case object Critical      extends ExplicitAlarmSeverity(5)

}
