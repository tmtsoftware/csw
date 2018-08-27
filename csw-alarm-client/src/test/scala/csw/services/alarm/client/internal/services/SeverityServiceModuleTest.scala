package csw.services.alarm.client.internal.services

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import com.typesafe.config.ConfigFactory
import csw.messages.params.models.Subsystem.{BAD, LGSF, NFIRAOS, TCS}
import csw.services.alarm.api.exceptions.{InactiveAlarmException, InvalidSeverityException, KeyNotFoundException}
import csw.services.alarm.api.models.AcknowledgementStatus.Unacknowledged
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.services.alarm.api.models.Key._
import csw.services.alarm.api.models.ShelveStatus._
import csw.services.alarm.api.models._
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.services.alarm.client.internal.helpers.{AlarmServiceTestSetup, TestDataFeeder}
import csw.services.alarm.client.internal.services.SeverityTestScenarios.{AckStatusTestCases, SeverityTestCases}

import scala.concurrent.duration.DurationInt

class SeverityServiceModuleTest
    extends AlarmServiceTestSetup
    with SeverityServiceModule
    with MetadataServiceModule
    with StatusServiceModule
    with TestDataFeeder {

  override protected def beforeEach(): Unit = {
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/more-alarms.conf")
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
  // DEOPSCSW-500: Update alarm time on current severity change
  test("setSeverity should latch alarm when it is higher than previous latched severity") {
    val status = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)

    status.acknowledgementStatus shouldBe Unacknowledged
    status.latchedSeverity shouldBe Major

    val status1 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Warning)
    status1.acknowledgementStatus shouldBe Unacknowledged
    status1.latchedSeverity shouldBe Major
    // current severity is changed, hence updated alarm time should be > old time
    status1.alarmTime.time should be > status.alarmTime.time

    val status2 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Warning)
    status2.acknowledgementStatus shouldBe Unacknowledged
    status2.latchedSeverity shouldBe Major
    // current severity is not changed, hence new alarm time == old time
    status2.alarmTime.time shouldEqual status1.alarmTime.time
  }

  // DEOPSCSW-444: Set severity api for component
  test("setSeverity should not auto-acknowledge alarm even when it is auto-acknowledgable") {
    val status = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)
    status.acknowledgementStatus shouldBe Unacknowledged
    status.latchedSeverity shouldBe Major
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  // DEOPSCSW-500: Update alarm time on current severity change
  test("setSeverity should not update alarm time when current severity does not change") {
    // latch it to major
    val status = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)
    // set the severity again to mimic alarm refreshing
    val status1 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)

    status.alarmTime.time shouldEqual status1.alarmTime.time
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
    setSeverity(tromboneAxisLowLimitAlarmKey, Major).await
    setSeverity(splitterLimitAlarmKey, Critical).await // splitter component should not be included

    val tromboneKey = ComponentKey(NFIRAOS, "trombone")
    getAggregatedSeverity(tromboneKey).await shouldBe Major
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should get aggregated severity for subsystem") {
    setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await
    setSeverity(tromboneAxisLowLimitAlarmKey, Major).await
    setSeverity(splitterLimitAlarmKey, Okay).await
    setSeverity(enclosureTempHighAlarmKey, Okay).await
    setSeverity(enclosureTempLowAlarmKey, Okay).await
    setSeverity(cpuExceededAlarmKey, Critical).await // TCS Alarm should not be included

    val tromboneKey = SubsystemKey(NFIRAOS)
    getAggregatedSeverity(tromboneKey).await shouldBe Major
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should get aggregated severity for global system") {
    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await
    setSeverity(tromboneAxisLowLimitAlarmKey, Okay).await
    setSeverity(splitterLimitAlarmKey, Okay).await
    setSeverity(enclosureTempHighAlarmKey, Indeterminate).await
    setSeverity(enclosureTempLowAlarmKey, Okay).await
    setSeverity(cpuExceededAlarmKey, Okay).await
    setSeverity(outOfRangeOffloadAlarmKey, Warning).await
    setSeverity(cpuIdleAlarmKey, Okay).await

    getAggregatedSeverity(GlobalKey).await shouldBe Indeterminate

    setSeverity(cpuExceededAlarmKey, Critical).await

    getAggregatedSeverity(GlobalKey).await shouldBe Critical
  }

  // DEOPSCSW-448: Set Activation status for an alarm entity
  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should not consider inactive alarms in aggregation") {
    enclosureTempHighAlarm.isActive shouldBe true
    setSeverity(enclosureTempHighAlarmKey, Indeterminate).await

    enclosureTempLowAlarm.isActive shouldBe false
    setSeverity(enclosureTempLowAlarmKey, Critical).await

    getAggregatedSeverity(ComponentKey(NFIRAOS, "enclosure")).await shouldBe Indeterminate
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should consider shelved alarms also in aggregation") {

    setStatus(cpuExceededAlarmKey, AlarmStatus().copy(shelveStatus = Unshelved))
    setSeverity(cpuExceededAlarmKey, Critical).await

    getAggregatedSeverity(ComponentKey(cpuExceededAlarmKey.subsystem, cpuExceededAlarmKey.component)).await shouldBe Critical
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should get aggregated to Disconnected for Warning and Disconnected severities") {
    setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await

    val tromboneKey = ComponentKey(NFIRAOS, "trombone")
    getAggregatedSeverity(tromboneKey).await shouldBe Disconnected
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should throw KeyNotFoundException when key is invalid") {
    val invalidAlarm = ComponentKey(BAD, "invalid")
    an[KeyNotFoundException] shouldBe thrownBy(getAggregatedSeverity(invalidAlarm).await)
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should throw InactiveAlarmException when all resolved keys are inactive") {
    val invalidAlarm = ComponentKey(LGSF, "tcsPkInactive")
    an[InactiveAlarmException] shouldBe thrownBy(getAggregatedSeverity(invalidAlarm).await)
  }

  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribe aggregated severity via callback for an alarm") {

    getAggregatedSeverity(GlobalKey).await shouldBe Disconnected

    // alarm subscription - nfiraos.trombone
    val testProbe         = TestProbe[FullAlarmSeverity]()(actorSystem.toTyped)
    val alarmSubscription = subscribeAggregatedSeverityCallback(tromboneAxisLowLimitAlarmKey, testProbe.ref ! _)
    alarmSubscription.ready().await

    Thread.sleep(500) // wait for redis connection to happen

    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    testProbe.expectMessage(Critical)
    testProbe.expectMessage(Disconnected) // severity expires after 1 second in test

    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await
    testProbe.expectNoMessage(200.millis)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribe aggregated severity via callback for a subsystem") {

    getAggregatedSeverity(GlobalKey).await shouldBe Disconnected

    // subsystem subscription - tcs
    val testProbe         = TestProbe[FullAlarmSeverity]()(actorSystem.toTyped)
    val alarmSubscription = subscribeAggregatedSeverityCallback(SubsystemKey(TCS), testProbe.ref ! _)
    alarmSubscription.ready().await

    Thread.sleep(500) // wait for redis connection to happen

    setSeverity(cpuExceededAlarmKey, Critical).await

    testProbe.expectMessage(Critical)
    testProbe.expectMessage(Disconnected) // severity expires after 1 second in test

    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await
    testProbe.expectNoMessage(200.millis)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribe aggregated severity via callback for two different subscriptions, one for a component and other for all") {

    getAggregatedSeverity(GlobalKey).await shouldBe Disconnected

    // component subscription - nfiraos.trombone
    val testProbe1         = TestProbe[FullAlarmSeverity]()(actorSystem.toTyped)
    val alarmSubscription1 = subscribeAggregatedSeverityCallback(ComponentKey(NFIRAOS, "trombone"), testProbe1.ref ! _)
    alarmSubscription1.ready().await

    // global subscription
    val testProbe2         = TestProbe[FullAlarmSeverity]()(actorSystem.toTyped)
    val alarmSubscription2 = subscribeAggregatedSeverityCallback(GlobalKey, testProbe2.ref ! _)
    alarmSubscription2.ready().await

    Thread.sleep(500) // wait for redis connection to happen

    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    testProbe1.expectMessage(Critical)
    testProbe1.expectMessage(Disconnected) // severity expires after 1 second in test

    testProbe2.expectMessage(Critical)
    testProbe2.expectMessage(Disconnected) // severity expires after 1 second in test

    setSeverity(splitterLimitAlarmKey, Critical).await

    testProbe2.expectMessage(Critical)
    testProbe2.expectMessage(Disconnected) // severity expires after 1 second in test

    testProbe1.expectNoMessage(200.millis)

    alarmSubscription1.unsubscribe().await
    alarmSubscription2.unsubscribe().await

    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    testProbe1.expectNoMessage(200.millis)
    testProbe2.expectNoMessage(200.millis)
  }

  // DEOPSCSW-448: Set Activation status for an alarm entity
  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribeAggregatedSeverityCallback should not consider inactive alarm for aggregation") {

    val testProbe = TestProbe[FullAlarmSeverity]()(actorSystem.toTyped)
    val alarmSubscription =
      subscribeAggregatedSeverityCallback(ComponentKey(NFIRAOS, "enclosure"), testProbe.ref ! _)
    alarmSubscription.ready().await

    Thread.sleep(500) // wait for redis connection to happen
    enclosureTempHighAlarm.isActive shouldBe true
    setSeverity(enclosureTempHighAlarmKey, Okay).await

    testProbe.expectMessage(Okay)

    setSeverity(enclosureTempHighAlarmKey, Okay).await

    enclosureTempLowAlarm.isActive shouldBe false
    setSeverity(enclosureTempLowAlarmKey, Critical).await

    testProbe.expectMessage(Okay)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-448: Set Activation status for an alarm entity
  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribeAggregatedSeverityCallback should throw InactiveAlarmException when all resolved keys are inactive") {

    an[InactiveAlarmException] shouldBe thrownBy {
      val subscription = subscribeAggregatedSeverityCallback(enclosureTempLowAlarmKey, println)
      subscription.ready().await
    }
  }

  // DEOPSCSW-448: Set Activation status for an alarm entity
  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribeAggregatedSeverity should throw KeyNotFoundException when key is invalid") {
    val invalidAlarm = ComponentKey(BAD, "invalid")
    an[KeyNotFoundException] shouldBe thrownBy {
      val subscription = subscribeAggregatedSeverityCallback(invalidAlarm, println)
      subscription.ready().await
    }
  }

  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribe aggregated severity via actorRef for a subsystem") {

    getAggregatedSeverity(GlobalKey).await shouldBe Disconnected

    // subsystem subscription - tcs
    val testProbe         = TestProbe[FullAlarmSeverity]()(actorSystem.toTyped)
    val alarmSubscription = subscribeAggregatedSeverityActorRef(SubsystemKey(TCS), testProbe.ref)
    alarmSubscription.ready().await

    Thread.sleep(500) // wait for redis connection to happen

    setSeverity(cpuExceededAlarmKey, Critical).await

    testProbe.expectMessage(Critical)
    testProbe.expectMessage(Disconnected) // severity expires after 1 second in test

    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await
    testProbe.expectNoMessage(200.millis)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-448: Set Activation status for an alarm entity
  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribeAggregatedSeverityActorRef should not consider inactive alarm for aggregation") {

    val testProbe         = TestProbe[FullAlarmSeverity]()(actorSystem.toTyped)
    val alarmSubscription = subscribeAggregatedSeverityActorRef(SubsystemKey(NFIRAOS), testProbe.ref)
    alarmSubscription.ready().await

    tromboneAxisLowLimitAlarm.isActive shouldBe true
    setSeverity(tromboneAxisLowLimitAlarmKey, Major).await

    testProbe.expectMessage(Major)

    setSeverity(tromboneAxisLowLimitAlarmKey, Major).await

    enclosureTempLowAlarm.isActive shouldBe false
    setSeverity(enclosureTempLowAlarmKey, Critical).await

    testProbe.expectMessage(Major)

    alarmSubscription.unsubscribe()
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("shelved alarms should be considered in aggregated severity") {
    val testProbe    = TestProbe[FullAlarmSeverity]()(actorSystem.toTyped)
    val componentKey = ComponentKey(cpuExceededAlarmKey.subsystem, cpuExceededAlarmKey.component)

    setStatus(cpuExceededAlarmKey, AlarmStatus().copy(shelveStatus = Shelved))

    val alarmSubscription = subscribeAggregatedSeverityCallback(componentKey, testProbe.ref ! _)
    alarmSubscription.ready().await
    Thread.sleep(500) // wait for redis connection to happen

    setSeverity(cpuExceededAlarmKey, Okay).await
    testProbe.expectMessage(Okay)

    setSeverity(cpuExceededAlarmKey, Critical).await
    testProbe.expectMessage(Critical)

    alarmSubscription.unsubscribe().await
  }

  //DEOPSCSW-444 : Set severity api for component
  SeverityTestCases.foreach { testCase ⇒
    test(testCase.name) {
      feedTestData(testCase)
      import testCase._

      //set severity to new Severity
      val status = setSeverityAndGetStatus(alarmKey, newSeverity)

      //get severity and assert
      status.latchedSeverity shouldEqual expectedLatchedSeverity
    }
  }

  // DEOPSCSW-444 : Set severity api for component
  // DEOPSCSW-496 : Set Ack status on setSeverity
  AckStatusTestCases.foreach { testCase ⇒
    test(testCase.name) {
      feedTestData(testCase)
      import testCase._

      getStatus(alarmKey).await.acknowledgementStatus shouldBe oldAckStatus

      //set severity to new Severity
      val status = setSeverityAndGetStatus(alarmKey, newSeverity)

      //get severity and assert
      status.acknowledgementStatus shouldEqual newAckStatus
    }
  }

  private def setSeverityAndGetStatus(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    setSeverity(alarmKey, alarmSeverity).await
    testStatusApi.get(alarmKey).await.get
  }
}
