package csw.services.alarm.api.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
 * Represents all the severities an alarm can have including Disconnected. The complete list of severities is Okay, Warning,
 * Major, Indeterminate, Disconnected, Critical.
 *
 * @param level is a fundamental value in comparing severities
 */
sealed abstract class FullAlarmSeverity private[alarm] (val level: Int) extends EnumEntry with Lowercase {

  /**
   * The name of Severity e.g. for `Major`, the name will be represented as `major`
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

  /**
   * Represents the disconnected state of an alarm. This severity is never set by a developer explicitly. Rather it is
   * inferred as `Disconnected` when the severity of an alarm expires and component is unable to refresh the severity.
   */
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

  /**
   * Represents the normal operation of the alarm
   */
  case object Okay extends AlarmSeverity(0)

  /**
   * Represents the warning state of an alarm for e.g. the alarm is raised during the observation night and it is expected
   * that day staff responds to it the following morning. It can be safely assumed that operation and performance of the
   * observation is not impacted by alarm raised with `Warning` severity.
   */
  case object Warning extends AlarmSeverity(1)

  /**
   * Represents the major state of an alarm for e.g the operator needs to respond to a major alarm within 30 to 60 minutes.
   * It can be safely assumed that major kind of alarms won't affect the observation operation but it may affect the
   * performance of the same.
   */
  case object Major extends AlarmSeverity(2)

  /**
   * Represents the indeterminate state of an alarm, for e.g. hardware is not able to update the state regularly and
   * hence component cannot determine the actual severity of the alarm.
   */
  case object Indeterminate extends AlarmSeverity(3)

  /**
   * Represents the critical state of an alarm for e.g. the operator needs to respond to a critical alarm within 30 minutes.
   * It can be safely assumed that operation cannot continue if a critical alarm is raised in the system.
   */
  case object Critical extends AlarmSeverity(5)
}
