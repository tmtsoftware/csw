package csw.services.alarm.api.models
import csw.services.alarm.api.internal.AlarmRW
import csw.services.alarm.api.models.ActivationStatus.Active
import upickle.default.{macroRW, ReadWriter ⇒ RW}

/**
 * Basic model for an AlarmMetadata.
 * This information is read from the Alarm Service Config File and stored in Redis
 * Indeterminate and Okay alarm severities are supported by all alarms.
 *
 * @param subsystem             alarm belongs to this subsystem
 * @param component             alarm belongs to this component
 * @param name                  name of the alarm
 * @param description           description of the alarm
 * @param location              description of where the alarming condition is located
 * @param alarmType             general category for the alarm (e.g., limit alarm)
 * @param supportedSeverities   severity levels implemented by the component alarm
 * @param probableCause         probable cause for each level or for all levels
 * @param operatorResponse      instructions or information to help the operator respond to the alarm.
 * @param isAutoAcknowledgeable represents whether alarm requires an acknowledgement by the operator or not
 * @param isLatchable           represents whether alarm is to be latched or not
 */
case class AlarmMetadata(
    subsystem: String,
    component: String,
    name: String,
    description: String,
    location: String,
    alarmType: AlarmType,
    private val supportedSeverities: Set[AlarmSeverity],
    probableCause: String,
    operatorResponse: String,
    isAutoAcknowledgeable: Boolean,
    isLatchable: Boolean,
    activationStatus: ActivationStatus
) {
  def alarmKey: AlarmKey                         = AlarmKey(subsystem, component, name)
  def isActive: Boolean                          = activationStatus == Active
  def allSupportedSeverities: Set[AlarmSeverity] = supportedSeverities ++ Set(AlarmSeverity.Indeterminate, AlarmSeverity.Okay)
}

object AlarmMetadata extends AlarmRW {

  // make sure Indeterminate and Okay severities are included in metadata while writing
  implicit val alarmMetadataRW: RW[AlarmMetadata] = macroRW[AlarmMetadata].bimap(
    identity,
    metadata ⇒ metadata.copy(supportedSeverities = metadata.allSupportedSeverities)
  )
}
