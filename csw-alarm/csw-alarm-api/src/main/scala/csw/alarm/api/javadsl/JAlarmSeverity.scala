package csw.alarm.api.javadsl

import csw.alarm.models.{AlarmSeverity, FullAlarmSeverity}

/**
 * Helper class for java to get the handle of `AlarmSeverity` which is fundamental to alarm service
 */
object JAlarmSeverity {

  /**
   * Represents the normal operation of the alarm
   */
  val Okay: AlarmSeverity = AlarmSeverity.Okay

  /**
   * Represents the warning state of an alarm for e.g. the alarm is raised during the observation night and it is expected
   * that day staff responds to it the following morning. It can be safely assumed that operation and performance of the
   * observation is not impacted by alarm raised with `Warning` severity.
   */
  val Warning: AlarmSeverity = AlarmSeverity.Warning

  /**
   * Represents the major state of an alarm for e.g the operator needs to respond to a major alarm within 30 to 60 minutes.
   * It can be safely assumed that major kind of alarms won't affect the observation operation but it may affect the
   * performance of the same.
   */
  val Major: AlarmSeverity = AlarmSeverity.Major

  /**
   * Represents the indeterminate state of an alarm, for e.g. hardware is not able to update the state regularly and
   * hence component cannot determine the actual severity of the alarm.
   */
  val Indeterminate: AlarmSeverity = AlarmSeverity.Indeterminate

  /**
   * Represents the disconnected state of an alarm. This severity is never set by a developer explicitly. Rather it is
   * inferred as `Disconnected` when the severity of an alarm expires and component is unable to refresh the severity.
   */
  val Disconnected: FullAlarmSeverity = FullAlarmSeverity.Disconnected

  /**
   * Represents the critical state of an alarm for e.g. the operator needs to respond to a critical alarm within 30 minutes.
   * It can be safely assumed that operation cannot continue if a critical alarm is raised in the system.
   */
  val Critical: AlarmSeverity = AlarmSeverity.Critical

}
