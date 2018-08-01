package csw.services.alarm.client.internal

import java.util.concurrent.CompletableFuture

import csw.services.alarm.api.javadsl.IAlarmService
import csw.services.alarm.api.models.{AlarmSeverity, Key}
import csw.services.alarm.api.scaladsl.AlarmService

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

class JAlarmServiceImpl(alarmService: AlarmService)(implicit ec: ExecutionContext) extends IAlarmService {
  override def setSeverity(key: Key.AlarmKey, severity: AlarmSeverity): CompletableFuture[Unit] =
    alarmService.setSeverity(key, severity).toJava.toCompletableFuture
}
