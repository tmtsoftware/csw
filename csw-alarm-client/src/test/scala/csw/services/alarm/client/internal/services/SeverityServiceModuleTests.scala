package csw.services.alarm.client.internal.services

import com.typesafe.config.ConfigFactory
import csw.messages.params.models.Subsystem.{AOESW, BAD, LGSF, NFIRAOS}
import csw.services.alarm.api.exceptions.{InactiveAlarmException, InvalidSeverityException, KeyNotFoundException}
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.services.alarm.api.models.ActivationStatus.Active
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.services.alarm.api.models.Key.{AlarmKey, ComponentKey, GlobalKey, SubsystemKey}
import csw.services.alarm.api.models.ShelveStatus._
import csw.services.alarm.api.models._
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.services.alarm.client.internal.helpers.{AlarmServiceTestSetup, SetSeverityAcknowledgementTestCase, SetSeverityTestCase}

class SeverityServiceModuleTests
    extends AlarmServiceTestSetup
    with SeverityServiceModule
    with MetadataServiceModule
    with StatusServiceModule {

  override protected def beforeEach(): Unit = {
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
    initAlarms(validAlarmsConfig, reset = true).await
  }

  // DEOPSCSW-444: Set severity api for component
  // DEOPSCSW-459: Update severity to Disconnected if not updated within predefined time
  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("setSeverity should set severity") {
    //set severity to Major
    val status = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)
    status.acknowledgementStatus shouldBe Unacknowledged
    status.latchedSeverity shouldBe Major
    status.shelveStatus shouldBe Unshelved
    status.alarmTime.isDefined shouldBe true

    //get severity and assert
    val alarmSeverity = testSeverityApi.get(tromboneAxisHighLimitAlarmKey).await.get
    alarmSeverity shouldEqual Major

    //wait for 1 second and assert expiry of severity
    Thread.sleep(1000)
    val severityAfter1Second = getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await
    severityAfter1Second shouldEqual Disconnected
  }

  // DEOPSCSW-444: Set severity api for component
  test("setSeverity should throw KeyNotFoundException when tried to set severity for key which does not exists in alarm store") {
    val invalidKey = AlarmKey(BAD, "trombone", "fakeAlarm")
    an[KeyNotFoundException] shouldBe thrownBy(setSeverity(invalidKey, Critical).await)
  }

  // DEOPSCSW-444: Set severity api for component
  test("setSeverity should throw InvalidSeverityException when unsupported severity is provided") {
    an[InvalidSeverityException] shouldBe thrownBy(setSeverity(tromboneAxisHighLimitAlarmKey, Critical).await)
  }

  // DEOPSCSW-444: Set severity api for component
  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("setSeverity should latch alarm when it is higher than previous latched severity") {
    val status = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)

    status.acknowledgementStatus shouldBe Unacknowledged
    status.latchedSeverity shouldBe Major
    status.alarmTime.isDefined shouldBe true

    val status1 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Warning)
    status1.acknowledgementStatus shouldBe Unacknowledged
    status1.latchedSeverity shouldBe Major
    // latched severity is not changed here hence alarm time should not change
    status1.alarmTime.get.time shouldEqual status.alarmTime.get.time

    val status2 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Okay)
    status2.acknowledgementStatus shouldBe Unacknowledged
    status2.latchedSeverity shouldBe Major
    status2.alarmTime.get.time shouldEqual status.alarmTime.get.time
  }

  // DEOPSCSW-444: Set severity api for component
  test("setSeverity should not auto-acknowledge alarm even when it is auto-acknowledgable") {
    val status = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)
    status.acknowledgementStatus shouldBe Unacknowledged
    status.latchedSeverity shouldBe Major
    status.alarmTime.isDefined shouldBe true
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("setSeverity should not update alarm time when latched severity does not change") {
    // latch it to major
    val status = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)
    // set the severity again to mimic alarm refreshing
    val status1 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)

    status.alarmTime.get.time shouldEqual status1.alarmTime.get.time
  }

  // DEOPSCSW-457: Fetch current alarm severity
  test("getCurrentSeverity should get current severity") {
    // Severity should be inferred to Disconnected when metadata exists but severity key does not exists in Alarm store.
    // This happens after bootstrapping Alarm store.
    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected
    setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await
    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Warning
  }

  // DEOPSCSW-457: Fetch current alarm severity
  test("getCurrentSeverity should throw exception if key does not exist") {
    val invalidAlarm = AlarmKey(BAD, "invalid", "invalid")
    an[KeyNotFoundException] shouldBe thrownBy(getCurrentSeverity(invalidAlarm).await)
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should get aggregated severity for component") {
    setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await
    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    val tromboneKey = ComponentKey(NFIRAOS, "trombone")
    getAggregatedSeverity(tromboneKey).await shouldBe Critical
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should get aggregated severity for subsystem") {
    setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await
    setSeverity(tromboneAxisLowLimitAlarmKey, Major).await

    val tromboneKey = SubsystemKey(NFIRAOS)
    getAggregatedSeverity(tromboneKey).await shouldBe Major
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should get aggregated severity for global system") {
    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    getAggregatedSeverity(GlobalKey).await shouldBe Critical
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should get aggregated to Disconnected for Warning and Disconnected severities") {
    setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await

    val tromboneKey = ComponentKey(NFIRAOS, "trombone")
    getAggregatedSeverity(tromboneKey).await shouldBe Disconnected
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should throw KeyNotFoundException when key is invalid") {
    val invalidAlarm = Key.ComponentKey(BAD, "invalid")
    an[KeyNotFoundException] shouldBe thrownBy(getAggregatedSeverity(invalidAlarm).await)
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should throw InactiveAlarmException when all resolved keys are inactive") {
    val invalidAlarm = Key.ComponentKey(LGSF, "tcsPkInactive")
    an[InactiveAlarmException] shouldBe thrownBy(getAggregatedSeverity(invalidAlarm).await)
  }

  val severityTestCases = List(
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

  //DEOPSCSW-444 : Set severity api for component
  severityTestCases.foreach(testCase => {
    test(testCase.toString) {

      feedTestData(testCase)

      //set severity to new Severity
      val status = setSeverityAndGetStatus(testCase.alarmKey, testCase.newSeverity)

      //get severity and assert
      status.latchedSeverity shouldEqual testCase.expectedLatchedSeverity
    }
  })

  val ackTestCases = List(
    SetSeverityAcknowledgementTestCase(
      alarmKey = AlarmKey(AOESW, "test", "LatchedSeverityChanged_TargetSeverityNotOkay"),
      oldSeverity = Warning,
      newSeverity = Critical,
      expectedAckStatus = Some(Unacknowledged)
    ),
    SetSeverityAcknowledgementTestCase(
      alarmKey = AlarmKey(AOESW, "test", "LatchedSeverityChanged_TargetSeverityOkay"),
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = None // no op
    ),
    SetSeverityAcknowledgementTestCase(
      alarmKey = AlarmKey(AOESW, "test", "LatchedSeverityNotChanged_TargetSeverityOkay"),
      oldSeverity = Warning,
      newSeverity = Okay,
      expectedAckStatus = None // no op
    ),
    SetSeverityAcknowledgementTestCase(
      alarmKey = AlarmKey(AOESW, "test", "LatchedSeverityNotChanged_TargetSeverityNotOkay"),
      oldSeverity = Critical,
      newSeverity = Warning,
      expectedAckStatus = None // no op
    )
  )

  //DEOPSCSW-444 : Set severity api for component
  ackTestCases.foreach(
    testCase =>
      test(testCase.toString) {

        feedTestData(testCase)

        val previousAckStatus = getStatus(testCase.alarmKey).await.acknowledgementStatus

        //set severity to new Severity
        val status = setSeverityAndGetStatus(testCase.alarmKey, testCase.newSeverity)

        //get severity and assert
        if (testCase.expectedAckStatus.isDefined)
          status.acknowledgementStatus shouldEqual testCase.expectedAckStatus.get
        else
          status.acknowledgementStatus shouldEqual previousAckStatus
    }
  )

  private def feedTestData(testCase: SetSeverityTestCase): Unit =
    feedTestData(testCase.alarmKey, testCase.oldSeverity)

  private def feedTestData(testCase: SetSeverityAcknowledgementTestCase): Unit =
    feedTestData(testCase.alarmKey, testCase.oldSeverity)

  private def feedTestData(alarmKey: AlarmKey, oldSeverity: FullAlarmSeverity): Unit = {
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
        isAutoAcknowledgeable = false,
        isLatchable = true,
        activationStatus = Active
      )
    ).await

    // Adding status for corresponding test in alarm store
    setStatus(alarmKey, AlarmStatus().copy(latchedSeverity = oldSeverity, acknowledgementStatus = Acknowledged)).await
  }

  private def setSeverityAndGetStatus(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    setSeverity(alarmKey, alarmSeverity).await
    testStatusApi.get(alarmKey).await.get
  }
}
