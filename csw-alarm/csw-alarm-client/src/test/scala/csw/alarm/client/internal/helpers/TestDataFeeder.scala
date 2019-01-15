package csw.alarm.client.internal.helpers
import csw.params.core.models.Subsystem.AOESW
import csw.alarm.api.models.AcknowledgementStatus.Acknowledged
import csw.alarm.api.models.ActivationStatus.Active
import csw.alarm.api.models.AlarmSeverity._
import csw.alarm.api.models.Key.AlarmKey
import csw.alarm.api.models._
import csw.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.alarm.client.internal.services.{MetadataServiceModule, SeverityServiceModule, StatusServiceModule}

trait TestDataFeeder {
  self: SeverityServiceModule with MetadataServiceModule with StatusServiceModule ⇒

  def feedTestData(testCase: SetSeverityTestCase): Unit =
    feedTestData(
      alarmKey = testCase.alarmKey,
      oldLatchedSeverity = testCase.oldLatchedSeverity
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
      oldAckStatus: AcknowledgementStatus = Acknowledged
  ): Unit = {
    // Adding metadata for corresponding test in alarm store
    setMetadata(
      alarmKey,
      AlarmMetadata(
        subsystem = AOESW,
        component = "test",
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
    setStatus(alarmKey, AlarmStatus().copy(acknowledgementStatus = oldAckStatus, latchedSeverity = oldLatchedSeverity)).await
  }

}
