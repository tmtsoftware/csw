package csw.services.alarm.api.javadsl

import java.util.concurrent.CompletableFuture

import csw.services.alarm.api.models.AlarmSeverity
import csw.services.alarm.api.models.Key.AlarmKey

trait IAlarmService {
  def setSeverity(key: AlarmKey, severity: AlarmSeverity): CompletableFuture[Unit]
}
