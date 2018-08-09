package csw.services.alarm.client.internal

import com.typesafe.config.ConfigFactory
import csw.services.alarm.api.exceptions.{InactiveAlarmException, InvalidSeverityException, KeyNotFoundException}
import csw.services.alarm.api.internal.SeverityKey
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, UnAcknowledged}
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.Key.{AlarmKey, ComponentKey, GlobalKey, SubsystemKey}
import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}
import csw.services.alarm.api.models.ShelveStatus.UnShelved
import csw.services.alarm.api.models.{Key, _}
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture

class AlarmServiceImplTest extends AlarmServiceTestSetup {

  override protected def beforeEach(): Unit = {
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
    alarmService.initAlarms(validAlarmsConfig, reset = true).await
  }

  // DEOPSCSW-444: Set severity api for component
  // DEOPSCSW-459: Update severity to Disconnected if not updated within predefined time
  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("setSeverity should set severity") {
    //set severity to Major
    val status = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)
    status.acknowledgementStatus shouldBe Acknowledged
    status.latchStatus shouldBe Latched
    status.latchedSeverity shouldBe Major
    status.shelveStatus shouldBe UnShelved
    status.alarmTime.isDefined shouldBe true

    //get severity and assert
    val alarmSeverity = testSeverityApi.get(tromboneAxisHighLimitAlarmKey).await.get
    alarmSeverity shouldEqual Major

    //wait for 1 second and assert expiry of severity
    Thread.sleep(1000)
    val severityAfter1Second = alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await
    severityAfter1Second shouldEqual Disconnected
  }

  // DEOPSCSW-444: Set severity api for component
  test("setSeverity should throw KeyNotFoundException when tried to set severity for key which does not exists in alarm store") {
    val invalidKey = AlarmKey("bad", "trombone", "fakeAlarm")
    intercept[KeyNotFoundException] {
      alarmService.setSeverity(invalidKey, AlarmSeverity.Critical).await
    }
  }

  // DEOPSCSW-444: Set severity api for component
  test("setSeverity should throw InvalidSeverityException when unsupported severity is provided") {
    intercept[InvalidSeverityException] {
      alarmService.setSeverity(tromboneAxisHighLimitAlarmKey, AlarmSeverity.Critical).await
    }
  }

  // DEOPSCSW-444: Set severity api for component
  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("setSeverity should latch alarm only when it is high risk and higher than latched severity in case of latchable alarms") {
    val status = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)

    status.acknowledgementStatus shouldBe Acknowledged
    status.latchStatus shouldBe Latched
    status.latchedSeverity shouldBe Major
    status.alarmTime.isDefined shouldBe true

    val status1 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Warning)
    status1.acknowledgementStatus shouldBe Acknowledged
    status1.latchStatus shouldBe Latched
    status1.latchedSeverity shouldBe Major
    status1.alarmTime.get.time shouldEqual status.alarmTime.get.time

    val status2 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Okay)
    status2.acknowledgementStatus shouldBe Acknowledged
    status2.latchStatus shouldBe Latched
    status2.latchedSeverity shouldBe Major
    status2.alarmTime.get.time shouldEqual status.alarmTime.get.time
  }

  // DEOPSCSW-444: Set severity api for component
  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("setSeverity should not latch alarm if it is not latchable") {
    val status = setSeverityAndGetStatus(cpuExceededAlarmKey, Critical)
    status.acknowledgementStatus shouldBe Acknowledged
    status.latchStatus shouldBe UnLatched
    status.latchedSeverity shouldBe Critical
    status.alarmTime.isDefined shouldBe true

    val status1 = setSeverityAndGetStatus(cpuExceededAlarmKey, Indeterminate)
    status1.acknowledgementStatus shouldBe Acknowledged
    status1.latchStatus shouldBe UnLatched
    status1.latchedSeverity shouldBe Indeterminate
    status1.alarmTime.get.time.isAfter(status.alarmTime.get.time)
  }

  // DEOPSCSW-444: Set severity api for component
  test("setSeverity should auto-acknowledge alarm only when it is auto-acknowledgable") {
    val status = setSeverityAndGetStatus(tromboneAxisLowLimitAlarmKey, Major)
    status.acknowledgementStatus shouldBe UnAcknowledged
    status.latchStatus shouldBe Latched
    status.latchedSeverity shouldBe Major
    status.alarmTime.isDefined shouldBe true
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("setSeverity should update alarm time only when severity changes for latchable alarms") {
    // latchable alarm
    val highLimitAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")

    // latch it to major
    val status = setSeverityAndGetStatus(highLimitAlarmKey, Major)

    // set the severity again to mimic alarm refreshing
    val status1 = setSeverityAndGetStatus(highLimitAlarmKey, Major)

    status.alarmTime.get.time shouldEqual status1.alarmTime.get.time
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("setSeverity should update alarm time only when severity changes for un-latchable alarms") {
    // un-latchable alarm
    val cpuExceededAlarm = AlarmKey("TCS", "tcsPk", "cpuExceededAlarm")

    // set severity to major
    val status = setSeverityAndGetStatus(cpuExceededAlarm, Major)

    // set the severity again to mimic alarm refreshing
    val status1 = setSeverityAndGetStatus(cpuExceededAlarm, Major)

    status.alarmTime.get.time shouldEqual status1.alarmTime.get.time
  }

  //  DEOPSCSW-445: Get api for alarm metadata
  test("getMetadata should fetch metadata of the given Alarm key") {
    alarmService.getMetadata(tromboneAxisHighLimitAlarmKey).await shouldBe tromboneAxisHighLimitAlarm
  }

  //  DEOPSCSW-445: Get api for alarm metadata
  test("getMetadata should throw exception while getting metadata if key does not exist") {
    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
    intercept[KeyNotFoundException] {
      alarmService.getMetadata(invalidAlarm).await
    }
  }

  // DEOPSCSW-463: Fetch Alarm List for a component name or pattern
  test("getMetadata should fetch all alarms for a component") {
    val tromboneKey = ComponentKey("TCS", "tcsPk")
    alarmService.getMetadata(tromboneKey).await should contain allElementsOf List(cpuExceededAlarm)
  }

  // DEOPSCSW-464: Fetch Alarm name list for a subsystem name or pattern
  test("getMetadata should fetch all alarms for a subsystem") {
    val nfiraosKey = SubsystemKey("nfiraos")
    alarmService.getMetadata(nfiraosKey).await should contain allElementsOf List(
      tromboneAxisHighLimitAlarm,
      tromboneAxisLowLimitAlarm
    )
  }

  // DEOPSCSW-464: Fetch Alarm name list for a subsystem name or pattern
  test("getMetadata should fetch all alarms of whole system") {
    val globalKey = GlobalKey
    alarmService.getMetadata(globalKey).await should contain allElementsOf List(
      tromboneAxisHighLimitAlarm,
      tromboneAxisLowLimitAlarm,
      cpuExceededAlarm
    )
  }

  // DEOPSCSW-464: Fetch Alarm name list for a subsystem name or pattern
  test("getMetadata should throw exception if no alarms are found while getting metadata by subsystem") {
    val invalidAlarm = SubsystemKey("invalid")
    intercept[KeyNotFoundException] {
      alarmService.getMetadata(invalidAlarm).await
    }
  }

  // DEOPSCSW-457: Fetch current alarm severity
  test("getCurrentSeverity should get current severity") {
    alarmService.setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await

    alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Warning
  }

  // DEOPSCSW-457: Fetch current alarm severity
  test("getCurrentSeverity should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
    intercept[KeyNotFoundException] {
      alarmService.getCurrentSeverity(invalidAlarm).await
    }
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should get aggregated severity for component") {
    alarmService.setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await
    alarmService.setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    val tromboneKey = ComponentKey("nfiraos", "trombone")
    alarmService.getAggregatedSeverity(tromboneKey).await shouldBe Critical
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test(
    "getAggregatedSeverity should get aggregated to Disconnected for Warning and Disconnected severities"
  ) {
    alarmService.setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await

    val tromboneKey = ComponentKey("nfiraos", "trombone")
    alarmService.getAggregatedSeverity(tromboneKey).await shouldBe Disconnected
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should throw KeyNotFoundException when key is invalid") {
    val invalidAlarm = Key.ComponentKey("invalid", "invalid")
    intercept[KeyNotFoundException] {
      alarmService.getAggregatedSeverity(invalidAlarm).await
    }
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should throw InactiveAlarmException when all resolved keys are inactive") {
    val invalidAlarm = Key.ComponentKey("LGSF", "tcsPkInactive")
    intercept[InactiveAlarmException] {
      alarmService.getAggregatedSeverity(invalidAlarm).await
    }
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
    alarmService.reset(highLimitAlarmKey).await
    val statusAfterReset = alarmService.getStatus(highLimitAlarmKey).await

    statusAfterReset.alarmTime.get.time.isAfter(status.alarmTime.get.time) shouldBe true
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("reset should update time only when severity changes for a latchable and not auto-acknowledgable alarm") {
    // latchable, not auto-acknowledgable alarm
    val lowLimitAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm")

    // latch it to okay
    val status = setSeverityAndGetStatus(lowLimitAlarmKey, Okay)

    alarmService.acknowledge(lowLimitAlarmKey).await

    // reset the alarm, which will make alarm to go to un-acknowledged
    alarmService.reset(lowLimitAlarmKey).await
    val statusAfterReset = alarmService.getStatus(lowLimitAlarmKey).await

    statusAfterReset.alarmTime.get.time shouldEqual status.alarmTime.get.time
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("reset should update time only when severity changes for an un-latchable and auto-acknowledgable alarm") {
    // un-latchable, auto-acknowledgable alarm
    val cpuExceededAlarm = AlarmKey("TCS", "tcsPk", "cpuExceededAlarm")

    // set current severity to okay, latched severity is also okay since alarm is un-latchable, alarm is acknowledged
    val status1 = setSeverityAndGetStatus(cpuExceededAlarm, Okay)

    // reset the alarm, which will make alarm to go to un-acknowledged, latched severity was already okay so no change there
    alarmService.reset(cpuExceededAlarm).await
    val statusAfterReset1 = alarmService.getStatus(cpuExceededAlarm).await

    // alarm time should be updated only when latched severity changes
    statusAfterReset1.alarmTime.get.time shouldEqual status1.alarmTime.get.time
  }

  //  test("reset should throw exception if key does not exist") {
  //    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
  //    intercept[KeyNotFoundException] {
  //      alarmService.reset(invalidAlarm)
  //    }
  //  }
  //
  //  test("reset should throw exception if severity is not okay") {
  //    val tromboneAxisLowLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm")
  //    intercept[ResetOperationNotAllowed] {
  //      alarmService.reset(tromboneAxisLowLimitAlarm)
  //    }
  //  }
  //
  //  test("getStatus should throw exception if key does not exist") {
  //    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
  //    intercept[KeyNotFoundException] {
  //      alarmService.getStatus(invalidAlarm)
  //    }
  //  }
  //
  //  test("acknowledge should acknowledge an alarm") {}
  //
  //  test("acknowledge should throw exception if key does not exist") {
  //    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
  //    intercept[KeyNotFoundException] {
  //      alarmService.acknowledge(invalidAlarm)
  //    }
  //  }
  //
  private def setSeverityAndGetStatus(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    alarmService.setSeverity(alarmKey, alarmSeverity).await
    testStatusApi.get(alarmKey).await.get
  }
}
