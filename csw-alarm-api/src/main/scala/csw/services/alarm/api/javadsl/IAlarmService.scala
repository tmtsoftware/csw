package csw.services.alarm.api.javadsl

import java.util.concurrent.CompletableFuture

import csw.services.alarm.api.models.AlarmSeverity
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.scaladsl.AlarmService

trait IAlarmService {
  def setSeverity(key: AlarmKey, severity: AlarmSeverity): CompletableFuture[Unit]
  def asScala: AlarmService
}
