package csw.services.alarm.client.internal.helpers
import csw.messages.params.models.Subsystem.AOESW
import csw.services.alarm.api.models.AcknowledgementStatus.Acknowledged
import csw.services.alarm.api.models.ActivationStatus.Active
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models._
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.services.alarm.client.internal.services.{MetadataServiceModule, SeverityServiceModule, StatusServiceModule}

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
