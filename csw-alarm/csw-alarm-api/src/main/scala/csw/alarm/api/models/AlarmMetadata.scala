package csw.alarm.api.models
import csw.alarm.api.models.ActivationStatus.Active
import csw.alarm.api.models.AlarmSeverity.{Indeterminate, Okay}
import csw.alarm.api.models.Key.AlarmKey
import csw.params.core.models.Subsystem

/**
 * Represents the metadata of an alarm e.g. name, subsystem it belongs to, supported severities, etc. This information is
 * read from the config file for alarms and stored in alarm store. Metadata is referred while setting the severity to validate
 * the operation. An alarm does not change it's metadata in it's entire life span.
 *
 * @note Indeterminate and Okay severities are supported by all alarms implicitly.
 */
case class AlarmMetadata private[alarm] (
    subsystem: Subsystem,
    component: String,
    name: String,
    description: String,
    location: String,
    alarmType: AlarmType,
    private[alarm] val supportedSeverities: Set[FullAlarmSeverity],
    probableCause: String,
    operatorResponse: String,
    isAutoAcknowledgeable: Boolean,
    isLatchable: Boolean,
    activationStatus: ActivationStatus
) {

  /**
   * Represents a unique alarm in the store
   *
   * @return an instance of AlarmKey composed of subsystem, component and name of the alarm
   */
  def alarmKey: AlarmKey = AlarmKey(subsystem, component, name)

  /**
   * Represents whether the alarm is active or not
   */
  def isActive: Boolean = activationStatus == Active

  /**
   * A collection of all severities the alarm can be raised to including Indeterminate and Okay
   */
  def allSupportedSeverities: Set[FullAlarmSeverity] = supportedSeverities ++ Set(Indeterminate, Okay)
}
