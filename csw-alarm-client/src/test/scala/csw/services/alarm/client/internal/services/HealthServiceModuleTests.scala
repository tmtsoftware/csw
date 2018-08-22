package csw.services.alarm.client.internal.services

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import com.typesafe.config.ConfigFactory
import csw.messages.params.models.Subsystem.{BAD, LGSF, NFIRAOS, TCS}
import csw.services.alarm.api.exceptions.{InactiveAlarmException, KeyNotFoundException}
import csw.services.alarm.api.models.AlarmHealth
import csw.services.alarm.api.models.AlarmHealth.{Bad, Good, Ill}
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.Key.{ComponentKey, GlobalKey, SubsystemKey}
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture

import scala.concurrent.duration.DurationLong

class HealthServiceModuleTests
    extends AlarmServiceTestSetup
    with HealthServiceModule
    with SeverityServiceModule
    with StatusServiceModule
    with MetadataServiceModule {

  override protected def beforeEach(): Unit = {
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
    initAlarms(validAlarmsConfig, reset = true).await
  }

  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should should get aggregated severity for a alarm") {
    setSeverity(tromboneAxisHighLimitAlarmKey, Okay).await
    getAggregatedHealth(tromboneAxisHighLimitAlarmKey).await shouldBe Good

    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await
    getAggregatedHealth(tromboneAxisHighLimitAlarmKey).await shouldBe Ill

    setSeverity(tromboneAxisHighLimitAlarmKey, Indeterminate).await
    getAggregatedHealth(tromboneAxisHighLimitAlarmKey).await shouldBe Bad
  }

  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should should get aggregated severity for a component") {
    setSeverity(tromboneAxisHighLimitAlarmKey, Okay).await
    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    val tromboneKey = ComponentKey(NFIRAOS, "trombone")
    getAggregatedHealth(tromboneKey).await shouldBe Bad
  }

  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should should get aggregated severity for a subsystem") {
    setSeverity(tromboneAxisHighLimitAlarmKey, Okay).await
    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    val tromboneKey = SubsystemKey(NFIRAOS)
    getAggregatedHealth(tromboneKey).await shouldBe Bad
  }

  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should should get aggregated severity for global system") {
    setSeverity(tromboneAxisHighLimitAlarmKey, Okay).await
    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    getAggregatedHealth(GlobalKey).await shouldBe Bad
  }

  // DEOPSCSW-466: Fetch alarm severity, component or subsystem
  test("getAggregatedHealth should throw KeyNotFoundException when key is invalid") {
    an[KeyNotFoundException] shouldBe thrownBy(getAggregatedHealth(SubsystemKey(BAD)).await)
  }

  // DEOPSCSW-466: Fetch alarm severity, component or subsystem
  test("getAggregatedHealth should throw InactiveAlarmException when all resolved keys are inactive") {
    an[InactiveAlarmException] shouldBe thrownBy(getAggregatedHealth(SubsystemKey(LGSF)).await)
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribe aggregated health via callback for an alarm") {

    getAggregatedHealth(GlobalKey).await shouldBe Bad

    // alarm subscription - nfiraos.trombone.tromboneAxisLowLimitAlarm
    val testProbe = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription =
      subscribeAggregatedHealthCallback(tromboneAxisLowLimitAlarmKey, testProbe.ref ! _)
    alarmSubscription.ready().await

    Thread.sleep(500) // wait for redis connection to happen

    setSeverity(tromboneAxisLowLimitAlarmKey, Major).await

    testProbe.expectMessage(Ill)
    testProbe.expectMessage(Bad) // severity expires after 1 second in test

    setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await
    testProbe.expectNoMessage(200.millis)

    alarmSubscription.unsubscribe().await

    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    testProbe.expectNoMessage(200.millis)
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribe aggregated health via callback for a subsystem") {

    getAggregatedHealth(GlobalKey).await shouldBe Bad

    // subsystem subscription - tcs
    val testProbe = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription =
      subscribeAggregatedHealthCallback(SubsystemKey(TCS), testProbe.ref ! _)
    alarmSubscription.ready().await

    Thread.sleep(500) // wait for redis connection to happen

    setSeverity(cpuExceededAlarmKey, Major).await

    testProbe.expectMessage(Ill)
    testProbe.expectMessage(Bad) // severity expires after 1 second in test

    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await
    testProbe.expectNoMessage(200.millis)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribe aggregated health via callback for a component") {

    getAggregatedHealth(GlobalKey).await shouldBe Bad

    // component subscription - nfiraos.trombone
    val testProbe         = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription = subscribeAggregatedHealthCallback(ComponentKey(NFIRAOS, "trombone"), testProbe.ref ! _)
    alarmSubscription.ready().await

    Thread.sleep(500) // wait for redis connection to happen

    setSeverity(tromboneAxisLowLimitAlarmKey, Major).await

    testProbe.expectMessage(Ill)
    testProbe.expectMessage(Bad) // severity expires after 1 second in test

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribe aggregated health via actorRef for a subsystem") {

    getAggregatedHealth(GlobalKey).await shouldBe Bad

    // subsystem subscription - tcs
    val testProbe = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription =
      subscribeAggregatedHealthActorRef(SubsystemKey(TCS), testProbe.ref)
    alarmSubscription.ready().await

    Thread.sleep(500) // wait for redis connection to happen

    setSeverity(cpuExceededAlarmKey, Okay).await

    testProbe.expectMessage(Good)
    testProbe.expectMessage(Bad) // severity expires after 1 second in test

    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await
    testProbe.expectNoMessage(200.millis)

    alarmSubscription.unsubscribe().await
  }
}
