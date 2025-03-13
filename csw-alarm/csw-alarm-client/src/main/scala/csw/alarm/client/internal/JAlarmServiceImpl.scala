/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client.internal

import java.util.concurrent.CompletableFuture

import org.apache.pekko.Done
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.models.{AlarmSeverity, Key}
import csw.alarm.api.scaladsl.AlarmService

import scala.jdk.FutureConverters.*

private[csw] class JAlarmServiceImpl(alarmService: AlarmService) extends IAlarmService {
  override def setSeverity(alarmKey: Key.AlarmKey, severity: AlarmSeverity): CompletableFuture[Done] =
    alarmService.setSeverity(alarmKey, severity).asJava.toCompletableFuture
  override def asScala: AlarmService = alarmService
}
