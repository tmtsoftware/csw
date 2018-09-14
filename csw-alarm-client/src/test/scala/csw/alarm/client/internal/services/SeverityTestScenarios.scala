package csw.alarm.client.internal.services
import csw.params.core.models.Subsystem.AOESW
import csw.alarm.api.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.alarm.api.models.AlarmSeverity.{Critical, Major, Okay, Warning}
import csw.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.alarm.api.models.Key.AlarmKey
import csw.alarm.client.internal.helpers.{SetSeverityAckStatusTestCase, SetSeverityTestCase}

object SeverityTestScenarios {

  val SeverityTestCases = List(
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "IsHigher_WasNotDisconnected"),
      oldLatchedSeverity = Warning,
      newSeverity = Critical,
      expectedLatchedSeverity = Critical
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "IsHigher_WasDisconnected"),
      oldLatchedSeverity = Disconnected,
      newSeverity = Critical,
      expectedLatchedSeverity = Critical
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "IsNotHigher_WasNotDisconnected"),
      oldLatchedSeverity = Major,
      newSeverity = Okay,
      expectedLatchedSeverity = Major
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "IsNotHigher_WasDisconnected"),
      oldLatchedSeverity = Disconnected,
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
      oldSeverity = Critical,
      newSeverity = Okay,
      isAutoAcknowledgeable = true,
      oldAckStatus = Unacknowledged,
      newAckStatus = Acknowledged
    ),
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(AOESW, "test", "alarm6"),
      oldSeverity = Okay,
      newSeverity = Okay,
      isAutoAcknowledgeable = true,
      oldAckStatus = Unacknowledged,
      newAckStatus = Acknowledged
    ),
    // ====== Severity = Okay && AutoAck = false =====
    // NewAckStatus = OldAckStatus
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(AOESW, "test", "alarm7"),
      oldSeverity = Warning,
      newSeverity = Okay,
      isAutoAcknowledgeable = false,
      oldAckStatus = Unacknowledged,
      newAckStatus = Unacknowledged
    ),
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(AOESW, "test", "alarm8"),
      oldSeverity = Warning,
      newSeverity = Okay,
      isAutoAcknowledgeable = false,
      oldAckStatus = Acknowledged,
      newAckStatus = Acknowledged
    )
  )

}
