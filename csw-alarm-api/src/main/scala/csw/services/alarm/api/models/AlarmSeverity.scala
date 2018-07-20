package csw.services.alarm.api.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed abstract class AlarmSeverity private[alarm] (val level: Int) extends EnumEntry with Lowercase {

  /**
   * The name of SeverityLevels e.g. for Major severity level, the name will be represented as `major`
   */
  def name: String = entryName

  def isHigherThan(otherSeverity: AlarmSeverity): Boolean = this.level > otherSeverity.level

  // Disconnected, Indeterminate and Okay are not considered as an alarm condition
  def isHighRisk: Boolean = this.level > 0
}

object AlarmSeverity extends Enum[AlarmSeverity] {

  /**
   * Returns a sequence of all alarm severity
   */
  def values: IndexedSeq[AlarmSeverity] = findValues

  case object Disconnected  extends AlarmSeverity(-2)
  case object Indeterminate extends AlarmSeverity(-1)
  case object Okay          extends AlarmSeverity(0)
  case object Warning       extends AlarmSeverity(1)
  case object Major         extends AlarmSeverity(2)
  case object Critical      extends AlarmSeverity(3)
}
