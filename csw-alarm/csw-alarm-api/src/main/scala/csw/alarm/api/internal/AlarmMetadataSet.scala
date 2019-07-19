package csw.alarm.api.internal

import csw.alarm.models.AlarmMetadata

private[alarm] case class AlarmMetadataSet(alarms: Set[AlarmMetadata])
