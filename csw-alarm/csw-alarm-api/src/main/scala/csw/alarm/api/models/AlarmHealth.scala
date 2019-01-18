package csw.alarm.api.models

import csw.alarm.api.models.AlarmSeverity._
import csw.alarm.api.models.FullAlarmSeverity.Disconnected
import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
 * Represent the health status e.g Good, Ill, Bad for an alarm/component/subsystem or whole system.
 */
sealed abstract class AlarmHealth extends EnumEntry with Lowercase

object AlarmHealth extends Enum[AlarmHealth] {

  /**
   * Returns a collection of `AlarmHealth` e.g Good, Ill or Bad
   */
  def values: IndexedSeq[AlarmHealth] = findValues

  /**
   * Represents health of an alarm/component/subsystem/system if all alarms are either Okay or Warning
   */
  case object Good extends AlarmHealth

  /**
   * Represents health of an alarm/component/subsystem/system if at-least one alarm is Major
   */
  case object Ill extends AlarmHealth

  /**
   * Represents health of an alarm/component/subsystem/system if at-least one alarm is either Disconnected, Indeterminate
   * or Critical
   */
  case object Bad extends AlarmHealth

  private[alarm] def fromSeverity(alarmSeverity: FullAlarmSeverity): AlarmHealth = alarmSeverity match {
    case Disconnected | Indeterminate | Critical => Bad
    case Major                                   => Ill
    case Okay | Warning                          => Good
  }
}
