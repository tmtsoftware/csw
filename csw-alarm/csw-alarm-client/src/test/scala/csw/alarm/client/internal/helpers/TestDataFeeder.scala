/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client.internal.helpers
import csw.alarm.client.internal.helpers.TestFutureExt.given
import scala.language.implicitConversions

import csw.alarm.client.internal.services.{MetadataServiceModule, SeverityServiceModule, StatusServiceModule}
import csw.alarm.models.AcknowledgementStatus.Acknowledged
import csw.alarm.models.ActivationStatus.Active
import csw.alarm.models.AlarmSeverity.*
import csw.alarm.models.Key.AlarmKey
import csw.alarm.models.*
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.AOESW

trait TestDataFeeder {
  self: SeverityServiceModule & MetadataServiceModule & StatusServiceModule =>

  def feedTestData(testCase: SetSeverityTestCase): Unit =
    feedTestData(
      alarmKey = testCase.alarmKey,
      oldLatchedSeverity = testCase.oldLatchedSeverity,
      initializing = testCase.initializing
    )

  def feedTestData(testCase: SetSeverityAckStatusTestCase): Unit =
    feedTestData(
      alarmKey = testCase.alarmKey,
      oldLatchedSeverity = testCase.oldSeverity,
      isAutoAck = testCase.isAutoAcknowledgeable,
      oldAckStatus = testCase.oldAckStatus
    )

  private def feedTestData(
      alarmKey: AlarmKey,
      oldLatchedSeverity: FullAlarmSeverity,
      isAutoAck: Boolean = false,
      oldAckStatus: AcknowledgementStatus = Acknowledged,
      initializing: Boolean = true
  ): Unit = {
    // Adding metadata for corresponding test in alarm store
    setMetadata(
      alarmKey,
      AlarmMetadata(
        prefix = Prefix(AOESW, "test"),
        name = alarmKey.name,
        description = "for test purpose",
        location = "testing",
        AlarmType.Absolute,
        Set(Okay, Warning, Major, Indeterminate, Critical),
        probableCause = "test",
        operatorResponse = "test",
        isAutoAcknowledgeable = isAutoAck,
        isLatchable = true,
        activationStatus = Active
      )
    ).await

    // Adding status for corresponding test in alarm store
    setStatus(
      alarmKey,
      AlarmStatus().copy(initializing = initializing, acknowledgementStatus = oldAckStatus, latchedSeverity = oldLatchedSeverity)
    ).await
  }

}
