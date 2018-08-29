package csw.services.alarm.api.models
import csw.messages.params.models.Subsystem
import csw.services.alarm.api.models.ActivationStatus.Active
import csw.services.alarm.api.models.AlarmSeverity.{Indeterminate, Okay}
import csw.services.alarm.api.models.Key.AlarmKey

/**
 * Basic model for an AlarmMetadata. This information is read from the Alarm Service Config File and stored in alarm store.
 * Indeterminate and Okay alarm severities are supported by all alarms implicitly. While setting the severity, metadata is
 * referred to validate the operation.
 *
 * @param subsystem alarm belongs to this subsystem
 * @param component alarm belongs to this component
 * @param name name of the alarm
 * @param description description of the alarm
 * @param location description of where the alarming condition is located
 * @param alarmType general category for the alarm (e.g., limit alarm)
 * @param supportedSeverities severity levels implemented by the component alarm
 * @param probableCause probable cause for each level or for all levels
 * @param operatorResponse instructions or information to help the operator respond to the alarm.
 * @param isAutoAcknowledgeable represents whether alarm requires an acknowledgement by the operator or not
 * @param isLatchable represents whether alarm is to be latched or not
 */
case class AlarmMetadata(
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
  def alarmKey: AlarmKey                             = AlarmKey(subsystem, component, name)
  def isActive: Boolean                              = activationStatus == Active
  def allSupportedSeverities: Set[FullAlarmSeverity] = supportedSeverities ++ Set(Indeterminate, Okay)
}
