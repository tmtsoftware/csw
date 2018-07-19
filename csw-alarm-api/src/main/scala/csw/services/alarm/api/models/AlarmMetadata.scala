package csw.services.alarm.api.models

case class AlarmMetadata(
    alarmKey: AlarmKey,
    description: String,
    location: String,
    alarmType: AlarmType,
    supportedSeverities: List[AlarmSeverity],
    probableCause: String,
    operatorResponse: String,
    isAutoAcknowledgable: Boolean,
    isLatchable: Boolean
)
