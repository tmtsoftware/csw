package csw.services.alarm.api.models

import csw.services.alarm.api.models.AlarmSeverity.Disconnected
import csw.services.alarm.api.models.ExplicitAlarmSeverity._
import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed abstract class AlarmHealth extends EnumEntry with Lowercase

object AlarmHealth extends Enum[AlarmHealth] {

  /**
   * Returns a sequence of all alarm severity
   */
  def values: IndexedSeq[AlarmHealth] = findValues

  case object Good extends AlarmHealth
  case object Ill  extends AlarmHealth
  case object Bad  extends AlarmHealth

  private[alarm] def fromSeverity(alarmSeverity: AlarmSeverity): AlarmHealth = alarmSeverity match {
    case Disconnected | Indeterminate | Critical => Bad
    case Major                                   => Ill
    case Okay | Warning                          => Good
  }
}
