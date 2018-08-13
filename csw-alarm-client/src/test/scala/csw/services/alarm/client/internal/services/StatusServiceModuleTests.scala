package csw.services.alarm.client.internal.services

import com.typesafe.config.ConfigFactory
import csw.services.alarm.api.exceptions.{KeyNotFoundException, ResetOperationNotAllowed}
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, UnAcknowledged}
import csw.services.alarm.api.models.AlarmSeverity.{Major, Okay, Warning}
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}
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
    // latchable, auto-acknowledgable alarm
    val highLimitAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")

    // latch it to major
    setSeverityAndGetStatus(highLimitAlarmKey, Major)

    // set the current severity to okay, latched severity is still at major
    val status = setSeverityAndGetStatus(highLimitAlarmKey, Okay)

    // reset the alarm, which sets the latched severity to okay
    reset(highLimitAlarmKey).await
    val statusAfterReset = getStatus(highLimitAlarmKey).await

    statusAfterReset.alarmTime.get.time.isAfter(status.alarmTime.get.time) shouldBe true
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("reset should update time only when severity changes for a latchable and not auto-acknowledgable alarm") {
    // latchable, not auto-acknowledgable alarm
    val lowLimitAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm")

    // latch it to okay
    val status = setSeverityAndGetStatus(lowLimitAlarmKey, Okay)

    acknowledge(lowLimitAlarmKey).await

    // reset the alarm, which will make alarm to go to un-acknowledged
    reset(lowLimitAlarmKey).await
    val statusAfterReset = getStatus(lowLimitAlarmKey).await

    statusAfterReset.alarmTime.get.time shouldEqual status.alarmTime.get.time
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("reset should update time only when severity changes for an un-latchable and auto-acknowledgable alarm") {
    // un-latchable, auto-acknowledgable alarm
    val cpuExceededAlarm = AlarmKey("TCS", "tcsPk", "cpuExceededAlarm")

    // set current severity to okay, latched severity is also okay since alarm is un-latchable, alarm is acknowledged
    val status1 = setSeverityAndGetStatus(cpuExceededAlarm, Okay)

    // reset the alarm, which will make alarm to go to acknowledged, un-latched severity was already okay so no change there
    reset(cpuExceededAlarm).await
    val statusAfterReset1 = getStatus(cpuExceededAlarm).await

    // alarm time should be updated only when latched severity changes
    statusAfterReset1.alarmTime.get.time shouldEqual status1.alarmTime.get.time
  }

  // DEOPSCSW-447: Reset api for alarm
  test("reset should set the alarm status to Unlatched Okay and Acknowledged when alarm is not latchable") {
    // un-latchable, auto-acknowledgable alarm
    val cpuExceededAlarm = AlarmKey("TCS", "tcsPk", "cpuExceededAlarm")

    // set current severity to okay, latched severity is also okay since alarm is un-latchable, alarm is acknowledged
    setSeverity(cpuExceededAlarm, Okay).await

    reset(cpuExceededAlarm).await
    val status = getStatus(cpuExceededAlarm).await
    status.latchedSeverity shouldEqual Okay
    status.latchStatus shouldEqual UnLatched
    status.acknowledgementStatus shouldEqual Acknowledged
  }

  // DEOPSCSW-447: Reset api for alarm
  test("reset should set the alarm status to Latched Okay and Acknowledged when alarm is latchable") {
    // latchable, not auto-acknowledgable alarm
    val lowLimitAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm")

    // set latched severity to Warning which will result status to be Latched and UnAcknowledged
    setSeverity(lowLimitAlarmKey, Warning).await

    // set current severity to Okay
    setSeverity(lowLimitAlarmKey, Okay).await

    reset(lowLimitAlarmKey).await
    val status = getStatus(lowLimitAlarmKey).await
    status.latchedSeverity shouldEqual Okay
    status.latchStatus shouldEqual Latched
    status.acknowledgementStatus shouldEqual Acknowledged
  }

  // DEOPSCSW-447: Reset api for alarm
  test("reset should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
    intercept[KeyNotFoundException] {
      reset(invalidAlarm).await
    }
  }

  // DEOPSCSW-447: Reset api for alarm
  test("reset should throw exception if severity is not okay") {
    val tromboneAxisLowLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm")
    intercept[ResetOperationNotAllowed] {
      reset(tromboneAxisLowLimitAlarm).await
    }
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("acknowledge should set acknowledgementStatus to Acknowledged of an alarm") {
    // latchable, not auto-acknowledgable alarm
    val lowLimitAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm")

    // set latched severity to Warning which will result status to be Latched and UnAcknowledged
    setSeverity(lowLimitAlarmKey, Warning).await

    acknowledge(lowLimitAlarmKey).await
    val status = getStatus(lowLimitAlarmKey).await
    status.acknowledgementStatus shouldBe Acknowledged
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("unAcknowledge should set acknowledgementStatus to UnAcknowledged of an alarm") {
    // latchable, not auto-acknowledgable alarm
    val lowLimitAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm")

    // set latched severity to Okay which will result status to be Latched and Acknowledged
    setSeverity(lowLimitAlarmKey, Okay).await

    unAcknowledge(lowLimitAlarmKey).await
    val status = getStatus(lowLimitAlarmKey).await
    status.acknowledgementStatus shouldBe UnAcknowledged
  }

  // DEOPSCSW-446: Acknowledge api for alarm
  test("acknowledge should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
    intercept[KeyNotFoundException] {
      acknowledge(invalidAlarm).await
    }
  }

  //  test("getStatus should throw exception if key does not exist") {
  //    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
  //    intercept[KeyNotFoundException] {
  //      getStatus(invalidAlarm)
  //    }
  //  }
  //
  private def setSeverityAndGetStatus(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    setSeverity(alarmKey, alarmSeverity).await
    testStatusApi.get(alarmKey).await.get
  }
}
