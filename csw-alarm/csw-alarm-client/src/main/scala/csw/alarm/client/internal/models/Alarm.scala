package csw.alarm.client.internal.models

import csw.alarm.api.models.Key.AlarmKey
import csw.alarm.api.models._

case class Alarm(key: AlarmKey, metadata: AlarmMetadata, status: AlarmStatus, severity: FullAlarmSeverity)
