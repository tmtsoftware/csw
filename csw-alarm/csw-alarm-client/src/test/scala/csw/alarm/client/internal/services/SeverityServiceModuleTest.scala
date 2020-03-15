package csw.alarm.client.internal.services

import java.net.InetAddress
import java.time.Instant

import akka.actor.testkit.typed.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.alarm.api.exceptions.{InactiveAlarmException, InvalidSeverityException, KeyNotFoundException}
import csw.alarm.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.alarm.models.AlarmSeverity._
import csw.alarm.models.FullAlarmSeverity.Disconnected
import csw.alarm.models.Key._
import csw.alarm.models.ShelveStatus._
import csw.alarm.models._
import csw.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.alarm.client.internal.helpers.{AlarmServiceTestSetup, TestDataFeeder}
import csw.alarm.client.internal.services.SeverityTestScenarios._
import csw.logging.models.Level.DEBUG
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.utils.TestAppender
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{CSW, LGSF, NFIRAOS, TCS}
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

//CSW-83:Alarm models should take prefix
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

  // DEOPSCSW-461: Log entry for severity update by component
  test("setCurrentSeverity should log a message | DEOPSCSW-461") {
    val logBuffer    = mutable.Buffer.empty[JsObject]
    val testAppender = new TestAppender(x => logBuffer += Json.parse(x.toString).as[JsObject])
    val hostName     = InetAddress.getLocalHost.getHostName

    val expectedMessage1 =
      "Setting severity [critical] for alarm [nfiraos-trombone-tromboneaxislowlimitalarm] with expire timeout [1] seconds"
    val expectedMessage2 = "Updating current severity [critical] in alarm store"

    val loggingSystem = new LoggingSystem("logging", "version", hostName, actorSystem)
    loggingSystem.setAppenders(List(testAppender))
    loggingSystem.setDefaultLogLevel(DEBUG)
    setCurrentSeverity(tromboneAxisLowLimitAlarmKey, AlarmSeverity.Critical).await
    Thread.sleep(200)
    val messages = logBuffer.map(log => log.getString("message"))
    messages.contains(expectedMessage1) shouldBe true
    messages.contains(expectedMessage2) shouldBe true

    loggingSystem.stop
  }

  // DEOPSCSW-444: Set severity api for component
  // DEOPSCSW-459: Update severity to Disconnected if not updated within predefined time
  test("setSeverity should set severity | DEOPSCSW-444, DEOPSCSW-459") {
    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected
    val status = getStatus(tromboneAxisHighLimitAlarmKey).await
    status.acknowledgementStatus shouldBe Acknowledged
    status.latchedSeverity shouldBe Disconnected
    status.shelveStatus shouldBe Unshelved

    //set severity to Major
    val status1 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)
    status1.acknowledgementStatus shouldBe Unacknowledged
    status1.latchedSeverity shouldBe Major
    status1.shelveStatus shouldBe Unshelved

    //get severity and assert
    val alarmSeverity = testSeverityApi.get(tromboneAxisHighLimitAlarmKey).await.get
    alarmSeverity shouldEqual Major

    //wait for 1 second and assert expiry of severity
    Thread.sleep(1000)
    val severityAfter1Second = getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await
    severityAfter1Second shouldEqual Disconnected
    settings.refreshInterval shouldBe 1.second
  }

  // DEOPSCSW-444: Set severity api for component
  test("setSeverity should throw KeyNotFoundException when tried to set severity for key which does not exists in alarm store | DEOPSCSW-444") {
    val invalidKey = AlarmKey(Prefix(TCS, "trombone"), "fakeAlarm")
    an[KeyNotFoundException] shouldBe thrownBy(setSeverity(invalidKey, Critical).await)
  }

  // DEOPSCSW-444: Set severity api for component
  test("setSeverity should throw InvalidSeverityException when unsupported severity is provided | DEOPSCSW-444") {
    an[InvalidSeverityException] shouldBe thrownBy(setSeverity(tromboneAxisHighLimitAlarmKey, Critical).await)
  }

  // DEOPSCSW-440: Model to represent Alarm Latched status
  // DEOPSCSW-444: Set severity api for component
  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  // DEOPSCSW-500: Update alarm time on current severity change
  test("setSeverity should latch alarm when it is higher than previous latched severity | DEOPSCSW-440, DEOPSCSW-444, DEOPSCSW-462, DEOPSCSW-500") {
    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected

    val status = getStatus(tromboneAxisHighLimitAlarmKey).await
    status.acknowledgementStatus shouldBe Acknowledged
    status.latchedSeverity shouldBe Disconnected
    status.shelveStatus shouldBe Unshelved

    val status1                     = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)
    val expectedRecordedTimeSpread1 = Instant.now.toEpochMilli +- 100
    status1.alarmTime.value.toEpochMilli shouldBe expectedRecordedTimeSpread1

    status1.acknowledgementStatus shouldBe Unacknowledged
    status1.latchedSeverity shouldBe Major

    Thread.sleep(200)

    val status2 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Warning)
    status2.acknowledgementStatus shouldBe Unacknowledged
    status2.latchedSeverity shouldBe Major
    // current severity is changed, hence updated alarm time should be > old time
    val expectedRecordedTimeSpread2 = Instant.now.toEpochMilli +- 100
    status2.alarmTime.value.toEpochMilli shouldBe expectedRecordedTimeSpread2
    status2.alarmTime.value should be > status1.alarmTime.value

    Thread.sleep(200)
    val status3 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Warning)
    status3.acknowledgementStatus shouldBe Unacknowledged
    status3.latchedSeverity shouldBe Major
    // current severity is not changed, hence new alarm time == old time
    status3.alarmTime.value shouldEqual status2.alarmTime.value
    status3.alarmTime.value.toEpochMilli shouldBe expectedRecordedTimeSpread2
  }

  // DEOPSCSW-444: Set severity api for component
  test("setSeverity should not auto-acknowledge alarm even when it is auto-acknowledgable | DEOPSCSW-444") {
    val status   = getStatus(tromboneAxisHighLimitAlarmKey).await
    val metadata = getMetadata(tromboneAxisHighLimitAlarmKey).await
    metadata.isAutoAcknowledgeable shouldBe true
    status.acknowledgementStatus shouldBe Acknowledged
    status.latchedSeverity shouldBe Disconnected

    val status1 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)
    status1.acknowledgementStatus shouldBe Unacknowledged
    status1.latchedSeverity shouldBe Major
  }

  // DEOPSCSW-462: Capture UTC timestamp in alarm state when severity is changed
  // DEOPSCSW-500: Update alarm time on current severity change
  test("setSeverity should not update alarm time when current severity does not change | DEOPSCSW-462, DEOPSCSW-500") {
    val status           = getStatus(tromboneAxisHighLimitAlarmKey).await
    val defaultAlarmTime = status.alarmTime

    // latch it to major
    val status1 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)
    status1.alarmTime.value should be > defaultAlarmTime.value
    val expectedRecordedTimeSpread1 = Instant.now.toEpochMilli +- 100
    status1.alarmTime.value.toEpochMilli shouldBe expectedRecordedTimeSpread1

    Thread.sleep(200)

    // set the severity again to mimic alarm refreshing
    val status2 = setSeverityAndGetStatus(tromboneAxisHighLimitAlarmKey, Major)
    status1.alarmTime.value shouldEqual status2.alarmTime.value
    status2.alarmTime.value.toEpochMilli shouldBe expectedRecordedTimeSpread1
  }

  // DEOPSCSW-457: Fetch current alarm severity
  test("getCurrentSeverity should get current severity | DEOPSCSW-457") {
    // Severity should be inferred to Disconnected when metadata exists but severity key does not exists in Alarm store.
    // This happens after bootstrapping Alarm store.
    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected
    setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await
    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Warning
  }

  // DEOPSCSW-457: Fetch current alarm severity
  test("getCurrentSeverity should throw exception if key does not exist | DEOPSCSW-457") {
    val invalidAlarm = AlarmKey(Prefix(CSW, "invalid"), "invalid")
    an[KeyNotFoundException] shouldBe thrownBy(getCurrentSeverity(invalidAlarm).await)
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should get aggregated severity for component | DEOPSCSW-465") {
    val tromboneKey = ComponentKey(Prefix(NFIRAOS, "trombone"))
    getAggregatedSeverity(tromboneKey).await shouldBe Disconnected

    setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await
    setSeverity(tromboneAxisLowLimitAlarmKey, Major).await
    setSeverity(splitterLimitAlarmKey, Critical).await // splitter component should not be included

    getAggregatedSeverity(tromboneKey).await shouldBe Major
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should get aggregated severity for subsystem | DEOPSCSW-465") {
    val tromboneKey = SubsystemKey(NFIRAOS)
    getAggregatedSeverity(tromboneKey).await shouldBe Disconnected

    setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await
    setSeverity(tromboneAxisLowLimitAlarmKey, Major).await
    setSeverity(splitterLimitAlarmKey, Okay).await
    setSeverity(enclosureTempHighAlarmKey, Okay).await
    setSeverity(enclosureTempLowAlarmKey, Okay).await
    setSeverity(cpuExceededAlarmKey, Critical).await // TCS Alarm should not be included

    getAggregatedSeverity(tromboneKey).await shouldBe Major
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should get aggregated severity for global system | DEOPSCSW-465") {
    getAggregatedSeverity(GlobalKey).await shouldBe Disconnected

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
  test("getAggregatedSeverity should not consider inactive alarms in aggregation | DEOPSCSW-448, DEOPSCSW-465") {
    getAggregatedSeverity(GlobalKey).await shouldBe Disconnected

    enclosureTempHighAlarm.isActive shouldBe true
    setSeverity(enclosureTempHighAlarmKey, Indeterminate).await

    enclosureTempLowAlarm.isActive shouldBe false
    setSeverity(enclosureTempLowAlarmKey, Critical).await

    getAggregatedSeverity(ComponentKey(Prefix(NFIRAOS, "enclosure"))).await shouldBe Indeterminate
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should consider shelved alarms also in aggregation | DEOPSCSW-449, DEOPSCSW-465") {
    shelve(cpuExceededAlarmKey).await
    getStatus(cpuExceededAlarmKey).await.shelveStatus shouldBe Shelved

    val componentKey = ComponentKey(cpuExceededAlarmKey.prefix)
    getAggregatedSeverity(componentKey).await shouldBe Disconnected

    // there is only one alarm in TCS.tcsPk component
    setSeverity(cpuExceededAlarmKey, Critical).await
    getAggregatedSeverity(componentKey).await shouldBe Critical
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should get aggregated to Disconnected for Warning and Critical severities | DEOPSCSW-465") {
    val tromboneKey = ComponentKey(Prefix(NFIRAOS, "trombone"))
    getAggregatedSeverity(tromboneKey).await shouldBe Disconnected
    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await
    setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await

    getAggregatedSeverity(tromboneKey).await shouldBe Critical
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should throw KeyNotFoundException when key is invalid | DEOPSCSW-465") {
    val invalidAlarm = ComponentKey(Prefix(CSW, "invalid"))
    an[KeyNotFoundException] shouldBe thrownBy(getAggregatedSeverity(invalidAlarm).await)
  }

  // DEOPSCSW-465: Fetch alarm severity, component or subsystem
  test("getAggregatedSeverity should throw InactiveAlarmException when all resolved keys are inactive | DEOPSCSW-465") {
    val invalidAlarm = ComponentKey(Prefix(LGSF, "tcspkinactive"))
    an[InactiveAlarmException] shouldBe thrownBy(getAggregatedSeverity(invalidAlarm).await)
  }

  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  // DEOPSCSW-494: Incorporate changes in set severity, reset, acknowledgement and latch status
  test("subscribe aggregated severity via callback for an alarm | DEOPSCSW-467, DEOPSCSW-494") {
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldBe Disconnected

    // alarm subscription - nfiraos.trombone
    val testProbe         = TestProbe[FullAlarmSeverity]()(actorSystem)
    val alarmSubscription = subscribeAggregatedSeverityCallback(tromboneAxisLowLimitAlarmKey, testProbe.ref ! _)
    alarmSubscription.ready().await
    testProbe.expectMessage(Disconnected) // on subscription, current aggregated severity will be calculated

    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    testProbe.expectMessage(Critical)
    testProbe.expectMessage(2.seconds, Disconnected) // severity expires after 1 second in test

    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await
    testProbe.expectNoMessage(200.millis) // Setting severity in another key doesn't affect this

    setSeverity(tromboneAxisLowLimitAlarmKey, Major).await
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldBe Major
    testProbe.expectMessage(Major)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribe aggregated severity via callback for a subsystem | DEOPSCSW-467") {
    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(cpuExceededAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(outOfRangeOffloadAlarmKey).await shouldBe Disconnected

    // subsystem subscription - tcs
    val testProbe         = TestProbe[FullAlarmSeverity]()(actorSystem)
    val alarmSubscription = subscribeAggregatedSeverityCallback(SubsystemKey(TCS), testProbe.ref ! _)
    alarmSubscription.ready().await
    testProbe.expectMessage(Disconnected) // on subscription, current aggregated severity will be calculated

    setSeverity(cpuExceededAlarmKey, Warning).await
    setSeverity(outOfRangeOffloadAlarmKey, Warning).await
    testProbe.expectMessage(Warning)

    // make sure that changing severity of non subscribed alarm does not contribute to aggregated severity calculation
    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await
    testProbe.expectNoMessage(200.millis)

    testProbe.expectMessage(2.seconds, Disconnected) // severity expires after 1 second in test

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribe aggregated severity via callback for two different subscriptions, one for a component and other for all | DEOPSCSW-467") {
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(splitterLimitAlarmKey).await shouldBe Disconnected

    // component subscription - nfiraos.trombone
    val testProbe1         = TestProbe[FullAlarmSeverity]()(actorSystem)
    val alarmSubscription1 = subscribeAggregatedSeverityCallback(ComponentKey(Prefix(NFIRAOS, "trombone")), testProbe1.ref ! _)
    alarmSubscription1.ready().await
    testProbe1.expectMessage(Disconnected) // on subscription, current aggregated severity will be calculated

    // global subscription
    val testProbe2         = TestProbe[FullAlarmSeverity]()(actorSystem)
    val alarmSubscription2 = subscribeAggregatedSeverityCallback(GlobalKey, testProbe2.ref ! _)
    alarmSubscription2.ready().await
    testProbe2.expectMessage(Disconnected) // on subscription, current aggregated severity will be calculated

    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    testProbe1.expectMessage(Critical)
    testProbe2.expectMessage(Critical)

    testProbe1.expectMessage(2.seconds, Disconnected) // severity expires after 1 second in test
    testProbe2.expectMessage(2.seconds, Disconnected) // severity expires after 1 second in test

    setSeverity(splitterLimitAlarmKey, Critical).await

    testProbe1.expectNoMessage(200.millis)
    testProbe2.expectMessage(Critical)

    testProbe2.expectMessage(2.seconds, Disconnected) // severity expires after 1 second in test

    alarmSubscription1.unsubscribe().await
    alarmSubscription2.unsubscribe().await

    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    testProbe1.expectNoMessage(200.millis)
    testProbe2.expectNoMessage(200.millis)
  }

  // DEOPSCSW-448: Set Activation status for an alarm entity
  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribeAggregatedSeverityCallback should not consider inactive alarm for aggregation | DEOPSCSW-448, DEOPSCSW-467") {
    getCurrentSeverity(enclosureTempHighAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(enclosureTempLowAlarmKey).await shouldBe Disconnected

    val testProbe         = TestProbe[FullAlarmSeverity]()(actorSystem)
    val alarmSubscription = subscribeAggregatedSeverityCallback(ComponentKey(Prefix(NFIRAOS, "enclosure")), testProbe.ref ! _)
    alarmSubscription.ready().await
    testProbe.expectMessage(Disconnected) // on subscription, current aggregated severity will be calculated

    enclosureTempHighAlarm.isActive shouldBe true
    setSeverity(enclosureTempHighAlarmKey, Okay).await
    testProbe.expectMessage(Okay) // enclosureTempLowAlarmKey=Inactive, hence not considered while aggregation

    setSeverity(enclosureTempHighAlarmKey, Okay).await
    testProbe.expectNoMessage(100.millis)

    enclosureTempLowAlarm.isActive shouldBe false
    setSeverity(enclosureTempLowAlarmKey, Critical).await

    // enclosureTempLowAlarm is inactive which is not considered for aggregation, that means no aggregated severity change
    testProbe.expectNoMessage(200.millis)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-448: Set Activation status for an alarm entity
  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribeAggregatedSeverityCallback should throw InactiveAlarmException when all resolved keys are inactive | DEOPSCSW-448, DEOPSCSW-467") {
    enclosureTempLowAlarm.isActive shouldBe false
    a[InactiveAlarmException] shouldBe thrownBy(
      subscribeAggregatedSeverityCallback(enclosureTempLowAlarmKey, println).ready().await
    )
  }

  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribeAggregatedSeverity should throw KeyNotFoundException when key is invalid | DEOPSCSW-467") {
    val invalidAlarm = ComponentKey(Prefix(CSW, "invalid"))
    a[KeyNotFoundException] shouldBe thrownBy(subscribeAggregatedSeverityCallback(invalidAlarm, println).ready().await)
  }

  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribe aggregated severity via actorRef for a subsystem | DEOPSCSW-467") {
    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(cpuExceededAlarmKey).await shouldBe Disconnected

    // subsystem subscription - tcs
    val testProbe         = TestProbe[FullAlarmSeverity]()(actorSystem)
    val alarmSubscription = subscribeAggregatedSeverityActorRef(SubsystemKey(TCS), testProbe.ref)
    alarmSubscription.ready().await
    testProbe.expectMessage(Disconnected) // on subscription, current aggregated severity will be calculated

    setSeverity(cpuExceededAlarmKey, Critical).await

    testProbe.expectMessage(Critical)
    testProbe.expectMessage(2.seconds, Disconnected) // severity expires after 1 second in test

    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await
    testProbe.expectNoMessage(200.millis)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-448: Set Activation status for an alarm entity
  // DEOPSCSW-467: Monitor alarm severities in the alarm store for a single alarm, component, subsystem, or all
  test("subscribeAggregatedSeverityActorRef should not consider inactive alarm for aggregation | DEOPSCSW-448, DEOPSCSW-467") {
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(enclosureTempLowAlarmKey).await shouldBe Disconnected

    val testProbe         = TestProbe[FullAlarmSeverity]()(actorSystem)
    val alarmSubscription = subscribeAggregatedSeverityActorRef(SubsystemKey(NFIRAOS), testProbe.ref)
    alarmSubscription.ready().await
    testProbe.expectMessage(Disconnected) // on subscription, current aggregated severity will be calculated

    tromboneAxisLowLimitAlarm.isActive shouldBe true
    setSeverity(tromboneAxisLowLimitAlarmKey, Major).await
    testProbe.expectNoMessage(100.millis) // other NFIRAOS keys are still disconnected, hence no change in aggregated severity

    enclosureTempLowAlarm.isActive shouldBe false
    setSeverity(enclosureTempLowAlarmKey, Critical).await
    testProbe.expectNoMessage(200.millis) //enclosureTempLowAlarmKey is inactive, hence aggregated severity is still Disconnected

    alarmSubscription.unsubscribe()
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("shelved alarms should be considered while subscribing aggregated severity | DEOPSCSW-449") {
    val testProbe = TestProbe[FullAlarmSeverity]()(actorSystem)
    // there is only one alarm in TCS.tcsPk component
    val componentKey = ComponentKey(cpuExceededAlarmKey.prefix)

    getCurrentSeverity(cpuExceededAlarmKey).await shouldBe Disconnected

    shelve(cpuExceededAlarmKey).await
    getStatus(cpuExceededAlarmKey).await.shelveStatus shouldBe Shelved

    val alarmSubscription = subscribeAggregatedSeverityCallback(componentKey, testProbe.ref ! _)
    alarmSubscription.ready().await
    testProbe.expectMessage(Disconnected) // on subscription, current aggregated severity will be calculated

    setSeverity(cpuExceededAlarmKey, Okay).await
    testProbe.expectMessage(Okay)

    setSeverity(cpuExceededAlarmKey, Critical).await
    testProbe.expectMessage(Critical)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-444: Set severity api for component
  // DEOPSCSW-494: Incorporate changes in set severity, reset, acknowledgement and latch status
  SeverityTestCases.foreach { testCase =>
    test(testCase.name) {
      feedTestData(testCase)
      import testCase._

      getStatus(alarmKey).await.latchedSeverity shouldBe oldLatchedSeverity

      //set severity to new Severity
      val status = setSeverityAndGetStatus(alarmKey, newSeverity)

      //get severity and assert
      status.latchedSeverity shouldEqual expectedLatchedSeverity
    }
  }

  // DEOPSCSW-444: Set severity api for component
  // DEOPSCSW-496: Set Ack status on setSeverity
  // DEOPSCSW-494: Incorporate changes in set severity, reset, acknowledgement and latch status
  AckStatusTestCases.foreach { testCase =>
    test(testCase.name()) {
      feedTestData(testCase)
      import testCase._

      getStatus(alarmKey).await.acknowledgementStatus shouldBe oldAckStatus

      //set severity to new Severity
      val status = setSeverityAndGetStatus(alarmKey, newSeverity)

      //get severity and assert
      status.acknowledgementStatus shouldEqual newAckStatus
    }
  }

  // DEOPSCSW-496 : Set Ack status on setSeverity
  AckStatusTestCasesForDisconnected.foreach { testCase =>
    test(testCase.name(Disconnected)) {
      feedTestData(testCase)
      import testCase._

      setCurrentSeverity(alarmKey, oldSeverity.asInstanceOf[AlarmSeverity])

      getStatus(alarmKey).await.acknowledgementStatus shouldBe oldAckStatus

      // severity expires after 1 second in test (wait 100 millis extra to make sure that it is expired and reflected in redis)
      Thread.sleep(1100)

      getCurrentSeverity(alarmKey).await shouldBe Disconnected

      val status = getStatus(alarmKey).await

      //get severity and assert
      status.acknowledgementStatus shouldEqual newAckStatus
    }
  }

  private def setSeverityAndGetStatus(alarmKey: AlarmKey, alarmSeverity: AlarmSeverity): AlarmStatus = {
    setSeverity(alarmKey, alarmSeverity).await
    getStatus(alarmKey).await
  }
}
