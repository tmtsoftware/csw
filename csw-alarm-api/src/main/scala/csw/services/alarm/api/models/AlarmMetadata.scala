package csw.services.alarm.api.models

case class AlarmMetadata(
    subsystem: String,
    component: String,
    name: String,
    description: String,
    location: String,
    alarmType: AlarmType,
    supportedSeverities: List[AlarmSeverity],
    probableCause: String,
    operatorResponse: String,
    isAutoAcknowledgeable: Boolean,
    isLatchable: Boolean
) {
  def alarmKey: AlarmKey = AlarmKey(subsystem, component, name)
}
