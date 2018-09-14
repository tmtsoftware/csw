package csw.alarm.client.internal

import java.util.concurrent.CompletableFuture

import akka.Done
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.api.models.{AlarmSeverity, Key}
import csw.alarm.api.scaladsl.AlarmService

import scala.compat.java8.FutureConverters.FutureOps

class JAlarmServiceImpl(alarmService: AlarmService) extends IAlarmService {
  override def setSeverity(alarmKey: Key.AlarmKey, severity: AlarmSeverity): CompletableFuture[Done] =
    alarmService.setSeverity(alarmKey, severity).toJava.toCompletableFuture
  override def asScala: AlarmService = alarmService
}
