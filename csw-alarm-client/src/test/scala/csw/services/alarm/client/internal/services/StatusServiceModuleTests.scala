package csw.services.alarm.client.internal.services

import com.typesafe.config.ConfigFactory
import csw.services.alarm.api.exceptions.{KeyNotFoundException, ResetOperationNotAllowed}
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, UnAcknowledged}
import csw.services.alarm.api.models.AlarmSeverity.{Major, Okay, Warning}
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}
import csw.services.alarm.api.models.ShelveStatus.{Shelved, UnShelved}
import csw.services.alarm.api.models.{AlarmSeverity, AlarmStatus}
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture

class StatusServiceModuleTests
    extends AlarmServiceTestSetup
    with StatusServiceModule
    with SeverityServiceModule
    with MetadataServiceModule {

  override protected def beforeEach(): Unit = {
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
    initAlarms(validAlarmsConfig, reset = true).await
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("reset should update time for a latchable and auto-acknowledgable alarm") {
    // latch it to major
    setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)

    // set the current severity to okay, latched severity is still at major
    val status = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Okay)

    // reset the alarm, which sets the latched severity to okay
    reset(tromboneAxisHighLimitAlarmKey).await
    val statusAfterReset = getStatus(tromboneAxisHighLimitAlarmKey).await

    statusAfterReset.alarmTime.get.time should be > status.alarmTime.get.time
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("reset should update time only when severity changes for a latchable and not auto-acknowledgeable alarm") {
    // latch it to okay
    val status = setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Okay)

    acknowledge(tromboneAxisLowLimitAlarmKey).await

    // reset the alarm, which will make alarm to go to un-acknowledged
    reset(tromboneAxisLowLimitAlarmKey).await
    val statusAfterReset = getStatus(tromboneAxisLowLimitAlarmKey).await

    statusAfterReset.alarmTime.get.time shouldEqual status.alarmTime.get.time
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("reset should update time only when severity changes for an un-latchable and auto-acknowledgable alarm") {
    // set current severity to okay, latched severity is also okay since alarm is un-latchable, alarm is acknowledged
    val status1 = setSeverityAndGetStatus(cpuExceededAlarmKey, Okay)

    // reset the alarm, which will make alarm to go to acknowledged, un-latched severity was already okay so no change there
    reset(cpuExceededAlarmKey).await
    val statusAfterReset1 = getStatus(cpuExceededAlarmKey).await

    // alarm time should be updated only when latched severity changes
    statusAfterReset1.alarmTime.get.time shouldEqual status1.alarmTime.get.time
  }

  // DEOPSCSW-447: Reset api for alarm
  test("reset should set the alarm status to Unlatched Okay and Acknowledged when alarm is not latchable") {
    // set current severity to okay, latched severity is also okay since alarm is un-latchable, alarm is acknowledged
    setSeverity(cpuExceededAlarmKey, Okay).await

    reset(cpuExceededAlarmKey).await
    val status = getStatus(cpuExceededAlarmKey).await
    status.latchedSeverity shouldEqual Okay
    status.latchStatus shouldEqual UnLatched
    status.acknowledgementStatus shouldEqual Acknowledged
  }

  // DEOPSCSW-447: Reset api for alarm
  test("reset should set the alarm status to Latched Okay and Acknowledged when alarm is latchable") {
    // set latched severity to Warning which will result status to be Latched and UnAcknowledged
    setSeverity(tromboneAxisLowLimitAlarmKey, Warning).await

    // set current severity to Okay
    setSeverity(tromboneAxisLowLimitAlarmKey, Okay).await

    reset(tromboneAxisLowLimitAlarmKey).await
    val status = getStatus(tromboneAxisLowLimitAlarmKey).await
    status.latchedSeverity shouldEqual Okay
    status.latchStatus shouldEqual Latched
    status.acknowledgementStatus shouldEqual Acknowledged
  }

  // DEOPSCSW-447: Reset api for alarm
  test("reset should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
    an[KeyNotFoundException] shouldBe thrownBy(reset(invalidAlarm).await)
  }

  // DEOPSCSW-447: Reset api for alarm
  test("reset should throw exception if severity is not okay") {
    an[ResetOperationNotAllowed] shouldBe thrownBy(reset(tromboneAxisLowLimitAlarmKey).await)
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("acknowledge should set acknowledgementStatus to Acknowledged of an alarm") {
    // set latched severity to Warning which will result status to be Latched and UnAcknowledged
    setSeverity(tromboneAxisLowLimitAlarmKey, Warning).await

    acknowledge(tromboneAxisLowLimitAlarmKey).await
    val status = getStatus(tromboneAxisLowLimitAlarmKey).await
    status.acknowledgementStatus shouldBe Acknowledged
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("unAcknowledge should set acknowledgementStatus to UnAcknowledged of an alarm") {
    // set latched severity to Okay which will result status to be Latched and Acknowledged
    setSeverity(tromboneAxisLowLimitAlarmKey, Okay).await

    unAcknowledge(tromboneAxisLowLimitAlarmKey).await
    val status = getStatus(tromboneAxisLowLimitAlarmKey).await
    status.acknowledgementStatus shouldBe UnAcknowledged
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("acknowledge should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
    an[KeyNotFoundException] shouldBe thrownBy(acknowledge(invalidAlarm).await)
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("shelve should shelve an alarm") {
    shelve(tromboneAxisHighLimitAlarmKey).await
    val status = getStatus(tromboneAxisHighLimitAlarmKey).await
    status.shelveStatus shouldBe Shelved
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("shelve should be a no-op when repeated") {
    shelve(tromboneAxisHighLimitAlarmKey).await
    val status = getStatus(tromboneAxisHighLimitAlarmKey).await
    status.shelveStatus shouldBe Shelved

    //repeat the shelve operation
    noException shouldBe thrownBy(shelve(tromboneAxisHighLimitAlarmKey).await)
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("unshelve should shelve an alarm") {
    setStatus(tromboneAxisHighLimitAlarmKey, AlarmStatus(shelveStatus = Shelved)).await
    unShelve(tromboneAxisHighLimitAlarmKey).await
    val status = getStatus(tromboneAxisHighLimitAlarmKey).await
    status.shelveStatus shouldBe UnShelved
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("unshelve should be a no-op when repeated") {
    setStatus(tromboneAxisHighLimitAlarmKey, AlarmStatus(shelveStatus = Shelved)).await
    unShelve(tromboneAxisHighLimitAlarmKey).await
    val status = getStatus(tromboneAxisHighLimitAlarmKey).await
    status.shelveStatus shouldBe UnShelved

    //repeat the unshelve operation
    noException shouldBe thrownBy(unShelve(tromboneAxisHighLimitAlarmKey).await)
  }

  //  test("getStatus should throw exception if key does not exist") {
  //    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
  //    an[KeyNotFoundException] shouldBe thrownBy(getStatus(invalidAlarm).await)
  //  }
  //

  private def setSeverityAndGetStatus(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    setSeverity(alarmKey, alarmSeverity).await
    testStatusApi.get(alarmKey).await.get
  }
}
