package csw.services.alarm.api.models
import csw.messages.params.models.Subsystem
import csw.services.alarm.api.models.ActivationStatus.Active
import csw.services.alarm.api.models.AlarmSeverity.{Indeterminate, Okay}
import csw.services.alarm.api.models.Key.AlarmKey

/**
 * Represents the metadata of an alarm e.g. name, subsystem it belongs to, supported severities, etc. This information is
 * read from the config file for alarms and stored in alarm store. Metadata is referred while setting the severity to validate
 * the operation. An alarm does not change it's metadata in it's entire life span.
 *
 * @note Indeterminate and Okay severities are supported by all alarms implicitly.
 * @param subsystem represents the subsystem this alarm belongs to
 * @param component represents the component this alarm belongs to
 * @param name represents the name of the alarm
 * @param description represents details and significance of the alarm
 * @param location represents the physical location of the alarm
 * @param alarmType represents the general category for the alarm (e.g. limit alarm)
 * @param supportedSeverities represents possible severity levels that the alarm can raise to (other then okay and indeterminate)
 * @param probableCause represents the probable cause for the alarm
 * @param operatorResponse represents instructions or information to help the operator respond to the alarm
 * @param isAutoAcknowledgeable represents whether alarm needs to be acknowledged when it goes to Okay severity
 * @param isLatchable represents whether the alarm can store the worst severity raised until reset
 * @param activationStatus represents whether the alarm is active or not
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
   * A collection of all severities the alarm can be raised to
   *
   * @return
   */
  def allSupportedSeverities: Set[FullAlarmSeverity] = supportedSeverities ++ Set(Indeterminate, Okay)
}
