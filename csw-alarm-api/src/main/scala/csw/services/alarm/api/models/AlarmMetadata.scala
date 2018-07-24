package csw.services.alarm.api.models

case class AlarmMetadata(
    alarmKey: AlarmKey,
    description: String,
    location: String,
    alarmType: AlarmType,
    supportedSeverities: List[AlarmSeverity],
    probableCause: String,
    operatorResponse: String,
    isAutoAcknowledgeable: Boolean,
    isLatchable: Boolean
)
