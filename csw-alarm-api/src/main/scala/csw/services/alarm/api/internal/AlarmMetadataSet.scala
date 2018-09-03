package csw.services.alarm.api.internal
import csw.services.alarm.api.models.AlarmMetadata

private[alarm] case class AlarmMetadataSet(alarms: Set[AlarmMetadata])
