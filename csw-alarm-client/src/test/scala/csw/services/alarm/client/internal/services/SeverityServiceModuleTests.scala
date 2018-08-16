package csw.services.alarm.client.internal.services

import com.typesafe.config.ConfigFactory
import csw.messages.params.models.Subsystem.{AOESW, BAD, LGSF, NFIRAOS}
import csw.services.alarm.api.exceptions.{InactiveAlarmException, InvalidSeverityException, KeyNotFoundException}
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.services.alarm.api.models.AlarmSeverity.Disconnected
import csw.services.alarm.api.models.ExplicitAlarmSeverity._
import csw.services.alarm.api.models.ActivationStatus.Active
import csw.services.alarm.api.models.Key.{AlarmKey, ComponentKey, GlobalKey, SubsystemKey}
import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}
import csw.services.alarm.api.models.ShelveStatus._
import csw.services.alarm.api.models._
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.services.alarm.client.internal.helpers.{AcknowledgeAndLatchTestCase, AlarmServiceTestSetup, SetSeverityTestCase}

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
    status.acknowledgementStatus shouldBe Acknowledged
    status.latchStatus shouldBe Latched
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
    // latched severity is not changed here hence alarm time should not change
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
    status.acknowledgementStatus shouldBe Unacknowledged
    status.latchStatus shouldBe Latched
    status.latchedSeverity shouldBe Major
    status.alarmTime.isDefined shouldBe true
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("setSeverity should update alarm time only when severity changes for latchable alarms") {
    // latch it to major
    val status = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)
    // set the severity again to mimic alarm refreshing
    val status1 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)

    status.alarmTime.get.time shouldEqual status1.alarmTime.get.time
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  test("setSeverity should update alarm time only when severity changes for un-latchable alarms") {
    // set severity to major
    val status = setSeverityAndGetStatus(cpuExceededAlarmKey, Major) // cpuExceededAlarmKey is un-latchable
    // set the severity again to mimic alarm refreshing
    val status1 = setSeverityAndGetStatus(cpuExceededAlarmKey, Major)

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

  val setSeverityTestCases: Array[SetSeverityTestCase] = Array(
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "low_latchable=>high_latchable"),
      oldSeverity = Warning,
      newSeverity = Major,
      expectedLatchedSeverity = Major
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "low_unlatable=>hig_latchable"),
      oldSeverity = Disconnected,
      newSeverity = Critical,
      expectedLatchedSeverity = Critical
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "high_latchable=>low_latchable"),
      oldSeverity = Major,
      newSeverity = Warning,
      expectedLatchedSeverity = Major
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(AOESW, "test_component", "high_unlatable=>low_latchable"),
      oldSeverity = Disconnected,
      newSeverity = Major,
      expectedLatchedSeverity = Major
    )
  )

  //DEOPSCSW-444 : Set severity api for component
  setSeverityTestCases.foreach(
    testCase =>
      test(testCase.toString) {

        // Adding metadata for corresponding test in alarm store

        setMetadata(
          testCase.alarmKey,
          AlarmMetadata(
            subsystem = testCase.alarmKey.subsystem,
            component = testCase.alarmKey.component,
            name = testCase.alarmKey.name,
            description = "for test purpose",
            location = "testing",
            AlarmType.Absolute,
            Set(Okay, Warning, Major, Indeterminate, Critical),
            probableCause = "test",
            operatorResponse = "test",
            isAutoAcknowledgeable = true,
            isLatchable = true,
            activationStatus = Active
          )
        ).await

        // Adding status for corresponding test in alarm store
        (testCase.alarmKey, testCase.oldSeverity) match {
          case (alarmKey, severity) if severity == Disconnected =>
            setStatus(alarmKey, AlarmStatus()).await
          case (alarmKey, severity) =>
            setStatus(
              alarmKey,
              AlarmStatus(
                latchedSeverity = severity,
                latchStatus = if (severity.latchable) Latched else UnLatched
              )
            ).await
        }

        //set severity to new Severity
        val status = setSeverityAndGetStatus(testCase.alarmKey, testCase.newSeverity)

        //get severity and assert
        status.latchedSeverity shouldEqual testCase.expectedLatchedSeverity
    }
  )

  val ackAndLatchTestCases = Array(
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "AutoAcknowledgeble_Latchable_Disconnected=>Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = true,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "NotAutoAcknowledgeble_Latchable_Disconnected=>Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = true,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "AutoAcknowledgeble_NotLatchable_Disconnected=>Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = false,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "NotAutoAcknowledgeble_NotLatchable_Disconnected=>Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = false,
      oldSeverity = Disconnected,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "AutoAcknowledgeble_Latchable_Okay=>Critical"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = true,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "NotAutoAcknowledgeble_Latchable_Okay=>Critical"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = true,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = Unacknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "AutoAcknowledgeble_NotLatchable_Okay=>Critical"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = false,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "NotAutoAcknowledgeble_NotLatchable_Okay=>Critical"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = false,
      oldSeverity = Okay,
      newSeverity = Critical,
      expectedAckStatus = Unacknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "AutoAcknowledgeble_Latchable_Critical=>Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = true,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "NotAutoAcknowledgeble_Latchable_Critical=>Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = true,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = Unacknowledged,
      expectedLatchStatus = Latched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "AutoAcknowledgeble_NotLatchable_Critical=>Okay"),
      isAutoAcknowledgeble = true,
      isAlarmLachable = false,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    ),
    AcknowledgeAndLatchTestCase(
      alarmKey = AlarmKey(AOESW, "test", "NotAutoAcknowledgeble_NotLatchable_Critical=>Okay"),
      isAutoAcknowledgeble = false,
      isAlarmLachable = false,
      oldSeverity = Critical,
      newSeverity = Okay,
      expectedAckStatus = Acknowledged,
      expectedLatchStatus = UnLatched
    )
  )

  //DEOPSCSW-444 : Set severity api for component
  ackAndLatchTestCases.foreach(
    testCase =>
      test(testCase.toString) {
        //test setup
        setMetadata(
          testCase.alarmKey,
          AlarmMetadata(
            subsystem = testCase.alarmKey.subsystem,
            component = testCase.alarmKey.component,
            name = testCase.alarmKey.name,
            description = "for test purpose",
            location = "testing",
            AlarmType.Absolute,
            Set(Okay, Warning, Major, Indeterminate, Critical),
            probableCause = "test",
            operatorResponse = "test",
            isAutoAcknowledgeable = testCase.isAutoAcknowledgeble,
            isLatchable = testCase.isAlarmLachable,
            activationStatus = Active
          )
        ).await

        setStatus(
          testCase.alarmKey,
          AlarmStatus(
            latchedSeverity = testCase.oldSeverity,
            acknowledgementStatus =
              if (testCase.isAutoAcknowledgeble || testCase.oldSeverity == Okay) Acknowledged else Unacknowledged,
            latchStatus = if (testCase.isAlarmLachable && testCase.oldSeverity.latchable) Latched else UnLatched
          )
        ).await

        //set severity to new Severity
        val status = setSeverityAndGetStatus(testCase.alarmKey, testCase.newSeverity)

        //get severity and assert
        status.acknowledgementStatus shouldEqual testCase.expectedAckStatus
        status.latchStatus shouldEqual testCase.expectedLatchStatus
    }
  )

  //  test("getStatus should throw exception if key does not exist") {
  //    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
  //    an[KeyNotFoundException] shouldBe thrownBy(getStatus(invalidAlarm).await)
  //  }
  //

  private def setSeverityAndGetStatus(alarmKey: AlarmKey, alarmSeverity: ExplicitAlarmSeverity): AlarmStatus = {
    setSeverity(alarmKey, alarmSeverity).await
    testStatusApi.get(alarmKey).await.get
  }
}
