package csw.services.alarm.client.internal
import java.io.File

import csw.services.alarm.api.exceptions.{InvalidSeverityException, KeyNotFoundException}
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, UnAcknowledged}
import csw.services.alarm.api.models.ActivationStatus.Active
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.AlarmType.Absolute
import csw.services.alarm.api.models.Key.{AlarmKey, ComponentKey, GlobalKey, SubsystemKey}
import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}
import csw.services.alarm.api.models.ShelveStatus.UnShelved
import csw.services.alarm.api.models._
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture

class AlarmServiceImplTest extends AlarmServiceTestSetup(26381, 6381) {

  override protected def beforeEach(): Unit = {
    val path = getClass.getResource("/test-alarms/valid-alarms.conf").getPath
    val file = new File(path)
    alarmService.initAlarms(file, reset = true).await
  }

  private val tromboneAxisHighLimitAlarm = AlarmMetadata(
    subsystem = "nfiraos",
    component = "trombone",
    name = "tromboneAxisHighLimitAlarm",
    description = "Warns when trombone axis has reached the high limit",
    location = "south side",
    AlarmType.Absolute,
    Set(Indeterminate, Okay, Warning, Major),
    probableCause = "the trombone software has failed or the stage was driven into the high limit",
    operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command",
    isAutoAcknowledgeable = true,
    isLatchable = true,
    activationStatus = Active
  )

  private val tromboneAxisLowLimitAlarm = AlarmMetadata(
    subsystem = "nfiraos",
    component = "trombone",
    name = "tromboneAxisLowLimitAlarm",
    description = "Warns when trombone axis has reached the low limit",
    location = "south side",
    alarmType = AlarmType.Absolute,
    Set(Warning, Major, Critical, Indeterminate, Okay),
    probableCause = "the trombone software has failed or the stage was driven into the low limit",
    operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command",
    isAutoAcknowledgeable = false,
    isLatchable = true,
    activationStatus = Active,
  )

  private val cpuExceededAlarm = AlarmMetadata(
    subsystem = "TCS",
    component = "tcsPk",
    name = "cpuExceededAlarm",
    description =
      "This alarm is activated when the tcsPk Assembly can no longer calculate all of its pointing values in the time allocated. The CPU may lock power, or there may be pointing loops running that are not needed. Response: Check to see if pointing loops are executing that are not needed or see about a more powerful CPU.",
    location = "in computer...",
    alarmType = Absolute,
    supportedSeverities = Set(Indeterminate, Okay, Warning, Major, Critical),
    probableCause = "too fast...",
    operatorResponse = "slow it down...",
    isAutoAcknowledgeable = true,
    isLatchable = false,
    activationStatus = Active
  )

  // DEOPSCSW-444: Set severity api for component
  // DEOPSCSW-459: Update severity to Disconnected if not updated within predefined time
  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("test set severity") {
    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
    //set severity to Major
    val status = setSeverity(tromboneAxisHighLimitAlarm, Major)
    status.acknowledgementStatus shouldBe Acknowledged
    status.latchStatus shouldBe Latched
    status.latchedSeverity shouldBe Major
    status.shelveStatus shouldBe UnShelved
    status.alarmTime.isDefined shouldBe true

    //get severity and assert
    val alarmSeverity = testSeverityApi.get(tromboneAxisHighLimitAlarm).await.get
    alarmSeverity shouldBe Major

    //wait for 1 second and assert expiry of severity
    Thread.sleep(1000)
    val severityAfter1Second = alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarm).await
    severityAfter1Second shouldBe Disconnected
  }

  // DEOPSCSW-444: Set severity api for component
  test("should throw InvalidSeverityException when unsupported severity is provided") {
    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")

    intercept[InvalidSeverityException] {
      alarmService.setSeverity(tromboneAxisHighLimitAlarm, AlarmSeverity.Critical).await
    }
  }

  // DEOPSCSW-444: Set severity api for component
  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("should not latch the alarm when it's latchable but not high risk") {
    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")

    //set severity to Okay
    val status = setSeverity(tromboneAxisHighLimitAlarm, Okay)
    status.latchStatus shouldBe UnLatched
    status.latchedSeverity shouldBe Okay
    status.alarmTime.isDefined shouldBe true

    //set severity to indeterminant
    val status1 = setSeverity(tromboneAxisHighLimitAlarm, Indeterminate)
    status1.latchStatus shouldBe UnLatched
    status1.latchedSeverity shouldBe Indeterminate
    status1.alarmTime.get.time.isAfter(status.alarmTime.get.time)
  }

  // DEOPSCSW-444: Set severity api for component
  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("should latch alarm only when it is high risk and higher than latched severity in case of latchable alarms") {
    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
    val status                     = setSeverity(tromboneAxisHighLimitAlarm, Major)

    status.acknowledgementStatus shouldBe Acknowledged
    status.latchStatus shouldBe Latched
    status.latchedSeverity shouldBe Major
    status.alarmTime.isDefined shouldBe true

    val status1 = setSeverity(tromboneAxisHighLimitAlarm, Warning)
    status1.acknowledgementStatus shouldBe Acknowledged
    status1.latchStatus shouldBe Latched
    status1.latchedSeverity shouldBe Major
    status1.alarmTime.get.time shouldEqual status.alarmTime.get.time

    val status2 = setSeverity(tromboneAxisHighLimitAlarm, Okay)
    status2.acknowledgementStatus shouldBe Acknowledged
    status2.latchStatus shouldBe Latched
    status2.latchedSeverity shouldBe Major
    status2.alarmTime.get.time shouldEqual status.alarmTime.get.time
  }

  // DEOPSCSW-444: Set severity api for component
  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("should not latch alarm if it is not latchable") {
    val cpuExceededAlarm = AlarmKey("TCS", "tcsPk", "cpuExceededAlarm")
    val status           = setSeverity(cpuExceededAlarm, Critical)
    status.acknowledgementStatus shouldBe Acknowledged
    status.latchStatus shouldBe UnLatched
    status.latchedSeverity shouldBe Critical
    status.alarmTime.isDefined shouldBe true

    val status1 = setSeverity(cpuExceededAlarm, Indeterminate)
    status1.acknowledgementStatus shouldBe Acknowledged
    status1.latchStatus shouldBe UnLatched
    status1.latchedSeverity shouldBe Indeterminate
    status1.alarmTime.get.time.isAfter(status.alarmTime.get.time)
  }

  // DEOPSCSW-444: Set severity api for component
  test("should auto-acknowledge alarm only when it is auto-acknowledgable while setting severity") {
    val tromboneAxisLowLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm")
    val status                    = setSeverity(tromboneAxisLowLimitAlarm, Major)
    status.acknowledgementStatus shouldBe UnAcknowledged
    status.latchStatus shouldBe Latched
    status.latchedSeverity shouldBe Major
    status.alarmTime.isDefined shouldBe true
  }

  //  DEOPSCSW-445: Get api for alarm metadata
  test("should fetch metadata of the given Alarm key") {
    val nfiraosAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
    alarmService.getMetadata(nfiraosAlarmKey).await shouldBe tromboneAxisHighLimitAlarm
  }

  //  DEOPSCSW-445: Get api for alarm metadata
  test("should throw exception while getting metadata if key does not exist") {
    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
    intercept[KeyNotFoundException] {
      alarmService.getMetadata(invalidAlarm).await
    }
  }

  //  DEOPSCSW-445: Get api for alarm metadata
  test("should fetch all alarms for a component") {
    val tromboneKey = ComponentKey("nfiraos", "trombone")
    alarmService.getMetadata(tromboneKey).await should contain allElementsOf List(
      tromboneAxisHighLimitAlarm,
      tromboneAxisLowLimitAlarm
    )
  }

  //  DEOPSCSW-445: Get api for alarm metadata
  test("should fetch all alarms for a subsystem") {
    val nfiraosKey = SubsystemKey("nfiraos")
    alarmService.getMetadata(nfiraosKey).await should contain allElementsOf List(
      tromboneAxisHighLimitAlarm,
      tromboneAxisLowLimitAlarm
    )
  }

  //  DEOPSCSW-445: Get api for alarm metadata
  test("should fetch all alarms of whole system") {
    val globalKey = GlobalKey
    alarmService.getMetadata(globalKey).await should contain allElementsOf List(
      tromboneAxisHighLimitAlarm,
      tromboneAxisLowLimitAlarm,
      cpuExceededAlarm
    )
  }

  //  DEOPSCSW-445: Get api for alarm metadata
  test("should throw exception if no alarms are found while getting metadata by subsystem") {
    val invalidAlarm = SubsystemKey("invalid")
    intercept[KeyNotFoundException] {
      alarmService.getMetadata(invalidAlarm).await
    }
  }

  test("should get current severity") {
    val nfiraosAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
    alarmService.setSeverity(nfiraosAlarmKey, Warning).await

    alarmService.getCurrentSeverity(nfiraosAlarmKey).await shouldBe Warning
  }

  test("should throw exception while getting current severity if key does not exist") {
    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
    intercept[KeyNotFoundException] {
      alarmService.getCurrentSeverity(invalidAlarm).await
    }
  }

  test("should get aggregated latched severity for component") {
    val highLimitAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
    alarmService.setSeverity(highLimitAlarmKey, Warning).await

    val lowLimitAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm")
    alarmService.setSeverity(lowLimitAlarmKey, Critical).await

    val tromboneKey = ComponentKey("nfiraos", "trombone")
    alarmService.getAggregatedSeverity(tromboneKey).await shouldBe Critical
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("should update time for a latchable and auto-acknowledgable alarm while resetting it") {
    // latchable, auto-acknowledgable alarm
    val highLimitAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")

    // latch it to major
    setSeverity(highLimitAlarmKey, Major)

    // set the current severity to okay, latched severity is still at major
    val status = setSeverity(highLimitAlarmKey, Okay)

    // reset the alarm, which sets the latched severity to okay
    alarmService.reset(highLimitAlarmKey).await
    val statusAfterReset = alarmService.getStatus(highLimitAlarmKey).await

    statusAfterReset.alarmTime.get.time.isAfter(status.alarmTime.get.time) shouldBe true
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("should update time only when severity changes for a latchable and not auto-acknowledgable alarm while resetting it ") {
    // latchable, not auto-acknowledgable alarm
    val lowLimitAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm")

    // latch it to okay
    val status = setSeverity(lowLimitAlarmKey, Okay)

    alarmService.acknowledge(lowLimitAlarmKey).await

    // reset the alarm, which will make alarm to go to un-acknowledged
    alarmService.reset(lowLimitAlarmKey).await
    val statusAfterReset = alarmService.getStatus(lowLimitAlarmKey).await

    statusAfterReset.alarmTime.get.time shouldEqual status.alarmTime.get.time
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("should update time only when severity changes for an un-latchable and auto-acknowledgable alarm while resetting it") {
    // un-latchable, auto-acknowledgable alarm
    val cpuExceededAlarm = AlarmKey("TCS", "tcsPk", "cpuExceededAlarm")

    // set current severity to okay, latched severity is also okay since alarm is un-latchable, alarm is acknowledged
    val status1 = setSeverity(cpuExceededAlarm, Okay)

    // reset the alarm, which will make alarm to go to un-acknowledged, latched severity was already okay so no change there
    alarmService.reset(cpuExceededAlarm).await
    val statusAfterReset1 = alarmService.getStatus(cpuExceededAlarm).await

    // alarm time should be updated only when latched severity changes
    statusAfterReset1.alarmTime.get.time shouldEqual status1.alarmTime.get.time
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("should update alarm time only when severity changes for latchable alarms while setting it") {
    // latchable alarm
    val highLimitAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")

    // latch it to major
    val status = setSeverity(highLimitAlarmKey, Major)

    // set the severity again to mimic alarm refreshing
    val status1 = setSeverity(highLimitAlarmKey, Major)

    status.alarmTime.get.time shouldEqual status1.alarmTime.get.time
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("should update alarm time only when severity changes for un-latchable alarms while setting it") {
    // un-latchable alarm
    val cpuExceededAlarm = AlarmKey("TCS", "tcsPk", "cpuExceededAlarm")

    // set severity to major
    val status = setSeverity(cpuExceededAlarm, Major)

    // set the severity again to mimic alarm refreshing
    val status1 = setSeverity(cpuExceededAlarm, Major)

    status.alarmTime.get.time shouldEqual status1.alarmTime.get.time
  }

//  test("should throw exception while getting status if key does not exist") {
//    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
//    intercept[KeyNotFoundException] {
//      alarmService.getStatus(invalidAlarm)
//    }
//  }
//
//  test("should throw exception while acknowledging alarm if key does not exist") {
//    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
//    intercept[KeyNotFoundException] {
//      alarmService.acknowledge(invalidAlarm)
//    }
//  }
//
//  test("should acknowledge an alarm") {}
//
//  test("should throw exception while resetting alarm if key does not exist") {
//    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
//    intercept[KeyNotFoundException] {
//      alarmService.reset(invalidAlarm)
//    }
//  }
//
//  test("should throw exception while resetting alarm if severity is not okay") {
//    val tromboneAxisLowLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisLowLimitAlarm")
//    intercept[ResetOperationNotAllowed] {
//      alarmService.reset(tromboneAxisLowLimitAlarm)
//    }
//  }
  private def setSeverity(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    alarmService.setSeverity(alarmKey, alarmSeverity).await
    testStatusApi.get(alarmKey).await.get
  }
}
