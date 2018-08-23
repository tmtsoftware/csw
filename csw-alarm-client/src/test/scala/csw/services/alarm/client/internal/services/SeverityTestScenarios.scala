package csw.services.alarm.client.internal.services
import csw.messages.params.models.Subsystem.AOESW
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.services.alarm.api.models.AlarmSeverity.{Critical, Major, Okay, Warning}
import csw.services.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.client.internal.helpers.{SetSeverityAckStatusTestCase, SetSeverityTestCase}

object SeverityTestScenarios {

  val SeverityTestCases = List(
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "IsHigher_WasNotDisconnected"),
      oldSeverity = Warning,
      newSeverity = Critical,
      expectedLatchedSeverity = Critical
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "IsHigher_WasDisconnected"),
      oldSeverity = Disconnected,
      newSeverity = Critical,
      expectedLatchedSeverity = Critical
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "IsNotHigher_WasNotDisconnected"),
      oldSeverity = Major,
      newSeverity = Okay,
      expectedLatchedSeverity = Major
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "IsNotHigher_WasDisconnected"),
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedLatchedSeverity = Okay
    )
  )

  val AckStatusTestCases = List(
    // ====== Severity change but not to Okay =====
    // AckStatus = Unacknowledged irrespective of AutoAck flag
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(AOESW, "test", "alarm1"),
      oldSeverity = Critical,
      newSeverity = Warning,
      isAutoAcknowledgeable = true,
      oldAckStatus = Unacknowledged,
      newAckStatus = Unacknowledged
    ),
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(AOESW, "test", "alarm2"),
      oldSeverity = Critical,
      newSeverity = Warning,
      isAutoAcknowledgeable = true,
      oldAckStatus = Acknowledged,
      newAckStatus = Unacknowledged
    ),
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(AOESW, "test", "alarm3"),
      oldSeverity = Critical,
      newSeverity = Warning,
      isAutoAcknowledgeable = false,
      oldAckStatus = Acknowledged,
      newAckStatus = Unacknowledged
    ),
    // ====== Severity = Okay && AutoAck = true =====
    // AckStatus = Acknowledged
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(AOESW, "test", "alarm4"),
      oldSeverity = Disconnected,
      newSeverity = Okay,
      isAutoAcknowledgeable = true,
      oldAckStatus = Unacknowledged,
      newAckStatus = Acknowledged
    ),
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(AOESW, "test", "alarm5"),
      oldSeverity = Okay,
      newSeverity = Okay,
      isAutoAcknowledgeable = true,
      oldAckStatus = Unacknowledged,
      newAckStatus = Acknowledged
    ),
    // ====== Severity = Okay && AutoAck = false =====
    // NewAckStatus = OldAckStatus
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(AOESW, "test", "alarm6"),
      oldSeverity = Warning,
      newSeverity = Okay,
      isAutoAcknowledgeable = false,
      oldAckStatus = Unacknowledged,
      newAckStatus = Unacknowledged
    ),
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(AOESW, "test", "alarm7"),
      oldSeverity = Warning,
      newSeverity = Okay,
      isAutoAcknowledgeable = false,
      oldAckStatus = Acknowledged,
      newAckStatus = Acknowledged
    )
  )

}
