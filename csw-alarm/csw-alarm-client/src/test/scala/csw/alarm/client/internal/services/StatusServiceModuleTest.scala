package csw.alarm.client.internal.services

import java.time.{Clock, LocalDateTime}

import com.typesafe.config.ConfigFactory
import csw.params.core.models.Subsystem
import csw.params.core.models.Subsystem.BAD
import csw.alarm.api.exceptions.KeyNotFoundException
import csw.alarm.api.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.alarm.api.models.AlarmSeverity._
import csw.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.alarm.api.models.Key.AlarmKey
import csw.alarm.api.models.ShelveStatus.{Shelved, Unshelved}
import csw.alarm.api.models.{AlarmSeverity, AlarmStatus}
import csw.alarm.client.internal.extensions.TimeExtensions
import csw.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.alarm.client.internal.helpers.TestFutureExt.RichFuture

class StatusServiceModuleTest
    extends AlarmServiceTestSetup
    with StatusServiceModule
    with SeverityServiceModule
    with MetadataServiceModule {

  override protected def beforeEach(): Unit = {
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
    initAlarms(validAlarmsConfig, reset = true).await
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  // DEOPSCSW-447: Reset api for alarm
  // DEOPSCSW-500: Update alarm time on current severity change
  test("reset should never update the alarm time") {
    // Initially latch and current are disconnected
    val defaultStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
    defaultStatus.latchedSeverity shouldEqual Disconnected
    defaultStatus.acknowledgementStatus shouldEqual Acknowledged
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    // Simulates initial component going to Okay and acknowledged status
    val status = setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Okay)
    status.latchedSeverity shouldBe Okay
    acknowledge(tromboneAxisLowLimitAlarmKey).await
    getStatus(tromboneAxisLowLimitAlarmKey).await.acknowledgementStatus shouldBe Acknowledged

    // reset the alarm and verify that time does not change
    reset(tromboneAxisLowLimitAlarmKey).await
    val statusAfterReset = getStatus(tromboneAxisLowLimitAlarmKey).await
    statusAfterReset.alarmTime.value shouldEqual status.alarmTime.value
  }

  // DEOPSCSW-447: Reset api for alarm
  test("simple test to check behavior of latch and reset") {
    getStatus(tromboneAxisLowLimitAlarmKey).await.latchedSeverity shouldEqual Disconnected
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    // Move over disconnected
    setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Okay).latchedSeverity shouldEqual Okay
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Okay

    // Now make a significant alarm and check latch
    setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Major).latchedSeverity shouldEqual Major
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Major

    // Return to normal
    setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Okay).latchedSeverity shouldEqual Major
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Okay

    // Now reset
    reset(tromboneAxisLowLimitAlarmKey).await
    getStatus(tromboneAxisLowLimitAlarmKey).await.latchedSeverity shouldBe Okay
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Okay

    // Check that it's not always okay
    setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Critical).latchedSeverity shouldEqual Critical
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Critical
    reset(tromboneAxisLowLimitAlarmKey).await
    getStatus(tromboneAxisLowLimitAlarmKey).await.latchedSeverity shouldBe Critical
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Critical
  }

  // DEOPSCSW-447: Reset api for alarm
  // DEOPSCSW-494: Incorporate changes in set severity, reset, acknowledgement and latch status
  List(Okay, Warning, Major, Indeterminate, Critical).foreach { currentSeverity =>
    test(s"reset should set latchedSeverity to current severity when current severity is $currentSeverity") {
      val defaultStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
      getMetadata(tromboneAxisLowLimitAlarmKey).await.isLatchable shouldBe true
      defaultStatus.latchedSeverity shouldEqual Disconnected
      defaultStatus.acknowledgementStatus shouldEqual Acknowledged
      getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

      // setup current severity - this does not change status
      // NOTE: This is different API than setSeverity which updates severity and status
      setCurrentSeverity(tromboneAxisLowLimitAlarmKey, currentSeverity).await
      getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldBe currentSeverity
      val previousStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
      previousStatus.acknowledgementStatus shouldEqual Acknowledged
      previousStatus.latchedSeverity shouldEqual Disconnected

      //reset alarm
      reset(tromboneAxisLowLimitAlarmKey).await

      val newStatus = getStatus(tromboneAxisLowLimitAlarmKey).await

      newStatus.latchedSeverity shouldEqual currentSeverity
      // autoAck=false for tromboneAxisLowLimitAlarmKey hence ackStatus will be same as before
      newStatus.acknowledgementStatus shouldEqual Acknowledged
    }
  }

  // DEOPSCSW-447: Reset api for alarm
  // DEOPSCSW-494: Incorporate changes in set severity, reset, acknowledgement and latch status
  test("reset should set latchedSeverity to current severity when current severity is Disconnected") {
    getMetadata(tromboneAxisLowLimitAlarmKey).await.isLatchable shouldBe true
    val defaultStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
    defaultStatus.latchedSeverity shouldEqual Disconnected
    defaultStatus.acknowledgementStatus shouldEqual Acknowledged
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    //set current and latched severity to warning
    setSeverity(tromboneAxisLowLimitAlarmKey, Warning).await

    val originalStatus = getStatus(tromboneAxisLowLimitAlarmKey).await

    originalStatus.latchedSeverity shouldEqual Warning
    originalStatus.acknowledgementStatus shouldEqual Unacknowledged
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Warning

    //wait for current severity to expire and get disconnected
    Thread.sleep(1500)
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    // This shows that until current severity goes to a real value or reset, latchedSeverity doesn't change
    // we are relying on future Alarm Server to do this change
    val notUpdatedStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
    notUpdatedStatus.latchedSeverity shouldEqual Warning

    //reset alarm
    reset(tromboneAxisLowLimitAlarmKey).await

    val newStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
    newStatus.acknowledgementStatus shouldEqual Acknowledged
    newStatus.latchedSeverity shouldEqual Disconnected
  }

  // DEOPSCSW-447: Reset api for alarm
  test("reset should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey(BAD, "invalid", "invalid")
    a[KeyNotFoundException] shouldBe thrownBy(reset(invalidAlarm).await)
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("acknowledge should set acknowledgementStatus to Acknowledged of an alarm") {
    getStatus(tromboneAxisLowLimitAlarmKey).await.acknowledgementStatus shouldEqual Acknowledged
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    // set latched severity to Warning which will result status to be Latched and Unacknowledged
    val status1 = setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Warning)
    status1.acknowledgementStatus shouldBe Unacknowledged

    acknowledge(tromboneAxisLowLimitAlarmKey).await
    val status = getStatus(tromboneAxisLowLimitAlarmKey).await
    status.acknowledgementStatus shouldBe Acknowledged
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("unacknowledge should set acknowledgementStatus to Unacknowledged of an alarm") {
    getStatus(tromboneAxisLowLimitAlarmKey).await.acknowledgementStatus shouldBe Acknowledged
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    // set latched severity to Okay which will result status to be Latched and Acknowledged
    setSeverity(tromboneAxisLowLimitAlarmKey, Okay).await
    getStatus(tromboneAxisLowLimitAlarmKey).await.acknowledgementStatus shouldBe Acknowledged

    unacknowledge(tromboneAxisLowLimitAlarmKey).await
    getStatus(tromboneAxisLowLimitAlarmKey).await.acknowledgementStatus shouldBe Unacknowledged
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("acknowledge should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey(BAD, "invalid", "invalid")
    a[KeyNotFoundException] shouldBe thrownBy(acknowledge(invalidAlarm).await)
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("unacknowledge should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey(BAD, "invalid", "invalid")
    a[KeyNotFoundException] shouldBe thrownBy(unacknowledge(invalidAlarm).await)
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("shelve should update the status of the alarm to shelved") {
    getStatus(tromboneAxisHighLimitAlarmKey).await.shelveStatus shouldBe Unshelved
    shelve(tromboneAxisHighLimitAlarmKey).await
    getStatus(tromboneAxisHighLimitAlarmKey).await.shelveStatus shouldBe Shelved

    //repeat the shelve operation
    noException shouldBe thrownBy(shelve(tromboneAxisHighLimitAlarmKey).await)
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("shelve alarm should automatically get unshelved on configured time") {
    // initially make sure the shelve status of alarm is unshelved
    getStatus(tromboneAxisLowLimitAlarmKey).await.shelveStatus shouldBe Unshelved

    val currentDateTime = LocalDateTime.now(Clock.systemUTC())
    // this will create shelveTimeout in 'h:m:s AM/PM' format which will be 2 seconds ahead from current time
    val shelveTimeout = currentDateTime.plusSeconds(2).format(TimeExtensions.TimeFormatter)

    shelve(tromboneAxisLowLimitAlarmKey, shelveTimeout).await
    getStatus(tromboneAxisLowLimitAlarmKey).await.shelveStatus shouldBe Shelved

    // after 2 seconds, expect that shelve gets timed out and inferred to unshelved
    Thread.sleep(2000)
    getStatus(tromboneAxisLowLimitAlarmKey).await.shelveStatus shouldBe Unshelved
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("unshelve should update the alarm status to unshelved") {
    // initialize alarm with Shelved status just for this test
    shelve(tromboneAxisHighLimitAlarmKey).await
    getStatus(tromboneAxisHighLimitAlarmKey).await.shelveStatus shouldBe Shelved

    unshelve(tromboneAxisHighLimitAlarmKey).await
    getStatus(tromboneAxisHighLimitAlarmKey).await.shelveStatus shouldBe Unshelved

    //repeat the unshelve operation
    noException shouldBe thrownBy(unshelve(tromboneAxisHighLimitAlarmKey).await)
  }

  test("getStatus should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey(Subsystem.BAD, "invalid", "invalid")
    a[KeyNotFoundException] shouldBe thrownBy(getStatus(invalidAlarm).await)
  }

  private def setSeverityAndGetStatus(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    setSeverity(alarmKey, alarmSeverity).await
    getStatus(alarmKey).await
  }
}
