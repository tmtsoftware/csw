package csw.services.alarm.client.internal.services

import com.typesafe.config.ConfigFactory
import csw.messages.params.models.Subsystem
import csw.messages.params.models.Subsystem.BAD
import csw.services.alarm.api.exceptions.KeyNotFoundException
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.ShelveStatus.{Shelved, Unshelved}
import csw.services.alarm.api.models.{AlarmSeverity, AlarmStatus}
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.services.alarm.client.internal.shelve.ShelveTimeoutMessage.{CancelShelveTimeout, ScheduleShelveTimeout}

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
  test("reset should not update time when severity does not change") {
    // latch it to okay
    val status = setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Okay)

    acknowledge(tromboneAxisLowLimitAlarmKey).await

    // reset the alarm, which will make alarm to go to un-acknowledged
    reset(tromboneAxisLowLimitAlarmKey).await
    val statusAfterReset = getStatus(tromboneAxisLowLimitAlarmKey).await

    statusAfterReset.alarmTime.time shouldEqual status.alarmTime.time
  }

  // DEOPSCSW-447: Reset api for alarm
  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  // DEOPSCSW-494: Incorporate changes in set severity, reset, acknowledgement and latch status
  List(Okay, Warning, Major, Indeterminate, Critical).foreach { currentSeverity =>
    test(s"reset should set latchedSeverity to current severity when current severity is $currentSeverity") {

      //setup current severity - this does not change status
      setCurrentSeverity(tromboneAxisLowLimitAlarmKey, currentSeverity).await

      val previousStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
      previousStatus.acknowledgementStatus shouldEqual Acknowledged
      previousStatus.latchedSeverity shouldEqual Disconnected

      //reset alarm
      reset(tromboneAxisLowLimitAlarmKey).await

      val newStatus = getStatus(tromboneAxisLowLimitAlarmKey).await

      newStatus.latchedSeverity shouldEqual currentSeverity
      newStatus.acknowledgementStatus shouldEqual Acknowledged
    }
  }

  // DEOPSCSW-447: Reset api for alarm
  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  // DEOPSCSW-494: Incorporate changes in set severity, reset, acknowledgement and latch status
  test("reset should set latchedSeverity to current severity when current severity is Disconnected") {
    //set current and latched severity to warning
    setSeverity(tromboneAxisLowLimitAlarmKey, Warning).await

    val originalStatus = getStatus(tromboneAxisLowLimitAlarmKey).await

    originalStatus.latchedSeverity shouldEqual Warning
    originalStatus.acknowledgementStatus shouldEqual Unacknowledged
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Warning

    //wait for current severity to expire and get disconnected
    Thread.sleep(1500)
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldEqual Disconnected

    //reset alarm
    reset(tromboneAxisLowLimitAlarmKey).await

    val newStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
    newStatus.acknowledgementStatus shouldEqual Acknowledged
    newStatus.latchedSeverity shouldEqual Disconnected
  }

  // DEOPSCSW-447: Reset api for alarm
  test("reset should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey(BAD, "invalid", "invalid")
    an[KeyNotFoundException] shouldBe thrownBy(reset(invalidAlarm).await)
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("acknowledge should set acknowledgementStatus to Acknowledged of an alarm") {
    // set latched severity to Warning which will result status to be Latched and Unacknowledged
    setSeverity(tromboneAxisLowLimitAlarmKey, Warning).await

    val status1 = getStatus(tromboneAxisLowLimitAlarmKey).await
    status1.acknowledgementStatus shouldBe Unacknowledged

    acknowledge(tromboneAxisLowLimitAlarmKey).await
    val status = getStatus(tromboneAxisLowLimitAlarmKey).await
    status.acknowledgementStatus shouldBe Acknowledged
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("unacknowledge should set acknowledgementStatus to Unacknowledged of an alarm") {
    val status1 = getStatus(tromboneAxisLowLimitAlarmKey).await
    status1.acknowledgementStatus shouldBe Acknowledged

    // set latched severity to Okay which will result status to be Latched and Acknowledged
    setSeverity(tromboneAxisLowLimitAlarmKey, Okay).await

    val status2 = getStatus(tromboneAxisLowLimitAlarmKey).await
    status2.acknowledgementStatus shouldBe Acknowledged

    unacknowledge(tromboneAxisLowLimitAlarmKey).await
    val status = getStatus(tromboneAxisLowLimitAlarmKey).await
    status.acknowledgementStatus shouldBe Unacknowledged
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("acknowledge should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey(BAD, "invalid", "invalid")
    an[KeyNotFoundException] shouldBe thrownBy(acknowledge(invalidAlarm).await)
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
  test("shelve should schedule a shelving timeout") {
    shelvingTimeoutProbe.receiveAll() //clear all messages
    shelve(tromboneAxisHighLimitAlarmKey).await
    getStatus(tromboneAxisHighLimitAlarmKey).await.shelveStatus shouldBe Shelved
    shelvingTimeoutProbe.receiveMessage() shouldBe ScheduleShelveTimeout(tromboneAxisHighLimitAlarmKey)
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("unshelve should update the alarm status to unshelved") {
    // initialize alarm with Shelved status just for this test
    setStatus(tromboneAxisHighLimitAlarmKey, AlarmStatus().copy(shelveStatus = Shelved)).await
    unshelve(tromboneAxisHighLimitAlarmKey).await
    getStatus(tromboneAxisHighLimitAlarmKey).await.shelveStatus shouldBe Unshelved

    //repeat the unshelve operation
    noException shouldBe thrownBy(unshelve(tromboneAxisHighLimitAlarmKey).await)
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("unshelve should cancel the shelving timeout scheduler") {
    // initialize alarm with Shelved status just for this test
    setStatus(tromboneAxisHighLimitAlarmKey, AlarmStatus().copy(shelveStatus = Shelved)).await
    shelvingTimeoutProbe.receiveAll() //clear all messages
    unshelve(tromboneAxisHighLimitAlarmKey).await
    getStatus(tromboneAxisHighLimitAlarmKey).await.shelveStatus shouldBe Unshelved
    shelvingTimeoutProbe.receiveMessage() shouldBe CancelShelveTimeout(tromboneAxisHighLimitAlarmKey)
  }

  test("getStatus should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey(Subsystem.BAD, "invalid", "invalid")
    an[KeyNotFoundException] shouldBe thrownBy(getStatus(invalidAlarm).await)
  }

  private def setSeverityAndGetStatus(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    setSeverity(alarmKey, alarmSeverity).await
    testStatusApi.get(alarmKey).await.get
  }
}
