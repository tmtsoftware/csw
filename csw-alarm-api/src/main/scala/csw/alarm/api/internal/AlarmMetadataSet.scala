package csw.alarm.api.internal
import csw.alarm.api.models.AlarmMetadata

private[alarm] case class AlarmMetadataSet(alarms: Set[AlarmMetadata])
