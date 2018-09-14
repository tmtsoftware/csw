package csw.alarm.client.internal.services

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import com.typesafe.config.ConfigFactory
import csw.params.core.models.Subsystem.{BAD, LGSF, NFIRAOS, TCS}
import csw.alarm.api.exceptions.{InactiveAlarmException, KeyNotFoundException}
import csw.alarm.api.models.AlarmHealth
import csw.alarm.api.models.AlarmHealth.{Bad, Good, Ill}
import csw.alarm.api.models.AlarmSeverity._
import csw.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.alarm.api.models.Key.{ComponentKey, GlobalKey, SubsystemKey}
import csw.alarm.api.models.ShelveStatus.Shelved
import csw.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.alarm.client.internal.helpers.TestFutureExt.RichFuture

import scala.concurrent.duration.DurationLong

class HealthServiceModuleTest
    extends AlarmServiceTestSetup
    with HealthServiceModule
    with SeverityServiceModule
    with StatusServiceModule
    with MetadataServiceModule {

  override protected def beforeEach(): Unit = {
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/more-alarms.conf")
    initAlarms(validAlarmsConfig, reset = true).await
  }

  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should get aggregated health for a alarm") {
    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected
    getAggregatedHealth(tromboneAxisHighLimitAlarmKey).await shouldBe Bad

    setSeverity(tromboneAxisHighLimitAlarmKey, Okay).await
    getAggregatedHealth(tromboneAxisHighLimitAlarmKey).await shouldBe Good

    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await
    getAggregatedHealth(tromboneAxisHighLimitAlarmKey).await shouldBe Ill

    setSeverity(tromboneAxisHighLimitAlarmKey, Indeterminate).await
    getAggregatedHealth(tromboneAxisHighLimitAlarmKey).await shouldBe Bad
  }

  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should get aggregated health for a component") {
    val tromboneKey = ComponentKey(NFIRAOS, "trombone")
    getAggregatedHealth(tromboneKey).await shouldBe Bad

    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldBe Disconnected

    setSeverity(tromboneAxisHighLimitAlarmKey, Okay).await
    setSeverity(tromboneAxisLowLimitAlarmKey, Major).await

    getAggregatedHealth(tromboneKey).await shouldBe Ill
  }

  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should get aggregated health for a subsystem") {
    val tcsKey = SubsystemKey(TCS)
    getAggregatedHealth(tcsKey).await shouldBe Bad

    getCurrentSeverity(cpuExceededAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(outOfRangeOffloadAlarmKey).await shouldBe Disconnected

    setSeverity(cpuExceededAlarmKey, Okay).await
    setSeverity(outOfRangeOffloadAlarmKey, Major).await

    getAggregatedHealth(tcsKey).await shouldBe Ill
  }

  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should get aggregated health for global system") {
    //feeding data of four alarms only, one inactive
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
    initAlarms(validAlarmsConfig, reset = true).await

    getAggregatedHealth(GlobalKey).await shouldBe Bad
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(cpuExceededAlarmKey).await shouldBe Disconnected

    setSeverity(tromboneAxisLowLimitAlarmKey, Okay).await
    setSeverity(tromboneAxisHighLimitAlarmKey, Okay).await
    setSeverity(cpuExceededAlarmKey, Okay).await

    getAggregatedHealth(GlobalKey).await shouldBe Good

    setSeverity(cpuExceededAlarmKey, Major).await

    getAggregatedHealth(GlobalKey).await shouldBe Ill
  }

  // DEOPSCSW-448: Set Activation status for an alarm entity
  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should not consider inactive alarms for health aggregation") {
    getAggregatedHealth(GlobalKey).await shouldBe Bad
    getCurrentSeverity(enclosureTempLowAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(enclosureTempHighAlarmKey).await shouldBe Disconnected

    enclosureTempHighAlarm.isActive shouldBe true
    setSeverity(enclosureTempHighAlarmKey, Okay).await

    enclosureTempLowAlarm.isActive shouldBe false
    setSeverity(enclosureTempLowAlarmKey, Critical).await

    val tromboneKey = ComponentKey(NFIRAOS, "enclosure")
    getAggregatedHealth(tromboneKey).await shouldBe Good
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should consider shelved alarms also for health aggregation") {
    val componentKey = ComponentKey(cpuExceededAlarmKey.subsystem, cpuExceededAlarmKey.component)
    getAggregatedHealth(componentKey).await shouldBe Bad

    shelve(cpuExceededAlarmKey).await
    getStatus(cpuExceededAlarmKey).await.shelveStatus shouldBe Shelved

    setSeverity(cpuExceededAlarmKey, Okay).await
    getAggregatedHealth(componentKey).await shouldBe Good

    setSeverity(cpuExceededAlarmKey, Critical).await
    getAggregatedHealth(componentKey).await shouldBe Bad
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
    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldBe Disconnected

    // alarm subscription - nfiraos.trombone.tromboneAxisLowLimitAlarm
    val testProbe         = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription = subscribeAggregatedHealthCallback(tromboneAxisLowLimitAlarmKey, testProbe.ref ! _)
    alarmSubscription.ready().await
    testProbe.expectMessage(Bad) // on subscription, current aggregated health will be calculated

    setSeverity(tromboneAxisLowLimitAlarmKey, Major).await
    testProbe.expectMessage(Ill)
    setSeverity(tromboneAxisLowLimitAlarmKey, Indeterminate).await
    testProbe.expectMessage(Bad)
    testProbe.expectNoMessage(2.seconds) // on severity expire event, you should not receive health = Bad event as it is already in Bad status

    setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await
    testProbe.expectNoMessage(200.millis)

    alarmSubscription.unsubscribe().await

    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    testProbe.expectNoMessage(200.millis)
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribe aggregated health via callback for a subsystem") {
    getAggregatedHealth(GlobalKey).await shouldBe Bad
    getCurrentSeverity(cpuExceededAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(outOfRangeOffloadAlarmKey).await shouldBe Disconnected

    // subsystem subscription - tcs
    val testProbe         = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription = subscribeAggregatedHealthCallback(SubsystemKey(TCS), testProbe.ref ! _)
    alarmSubscription.ready().await
    testProbe.expectMessage(Bad) // on subscription, current aggregated health will be calculated

    setSeverity(cpuExceededAlarmKey, Major).await
    testProbe.expectNoMessage(100.millis) // outOfRangeOffloadAlarmKey is still disconnected, hence aggregated heath = Bad which means no change

    setSeverity(outOfRangeOffloadAlarmKey, Warning).await
    testProbe.expectMessage(Ill) // outOfRangeOffloadAlarmKey=Warning and cpuExceededAlarmKey=Major, hence aggregated heath = Ill

    setSeverity(outOfRangeOffloadAlarmKey, Warning).await
    testProbe.expectNoMessage(200.millis)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribe aggregated health via callback for a component") {
    getAggregatedHealth(GlobalKey).await shouldBe Bad
    getCurrentSeverity(tromboneAxisLowLimitAlarmKey).await shouldBe Disconnected

    // component subscription - nfiraos.trombone
    val testProbe         = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription = subscribeAggregatedHealthCallback(ComponentKey(NFIRAOS, "trombone"), testProbe.ref ! _)
    alarmSubscription.ready().await
    testProbe.expectMessage(Bad) // on subscription, current aggregated health will be calculated

    setSeverity(tromboneAxisLowLimitAlarmKey, Major).await
    testProbe.expectNoMessage(100.millis) // tromboneAxisHighLimitAlarmKey is still disconnected, hence aggregated heath = Bad which means no change

    setSeverity(tromboneAxisHighLimitAlarmKey, Warning).await
    testProbe.expectMessage(Ill) //tromboneAxisHighLimitAlarmKey=Warning & tromboneAxisLowLimitAlarmKey=Major

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-448: Set Activation status for an alarm entity
  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribeAggregatedHealthCallback should not consider inactive alarm for aggregation") {
    getAggregatedHealth(GlobalKey).await shouldBe Bad
    getCurrentSeverity(enclosureTempHighAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(enclosureTempLowAlarmKey).await shouldBe Disconnected

    val testProbe = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription =
      subscribeAggregatedHealthCallback(ComponentKey(NFIRAOS, "enclosure"), testProbe.ref ! _)
    alarmSubscription.ready().await
    testProbe.expectMessage(Bad) // on subscription, current aggregated health will be calculated

    enclosureTempHighAlarm.isActive shouldBe true
    setSeverity(enclosureTempHighAlarmKey, Okay).await
    testProbe.expectMessage(Good)

    setSeverity(enclosureTempHighAlarmKey, Okay).await
    testProbe.expectNoMessage(100.millis)

    enclosureTempLowAlarm.isActive shouldBe false
    setSeverity(enclosureTempLowAlarmKey, Critical).await

    // enclosureTempLowAlarmKey is inactive hence changing its severity does not change health
    testProbe.expectNoMessage(200.millis)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribeAggregatedSeverityCallback should throw KeyNotFoundException if the key does not match any key") {
    a[KeyNotFoundException] shouldBe thrownBy(subscribeAggregatedHealthCallback(SubsystemKey(BAD), println).ready().await)
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribeAggregatedHealthCallback should throw KeyNotFoundException when key is invalid") {
    a[KeyNotFoundException] shouldBe thrownBy(subscribeAggregatedHealthCallback(SubsystemKey(BAD), println).ready().await)
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribeAggregatedHealthCallback should throw InactiveAlarmException when all resolved keys are inactive") {
    a[InactiveAlarmException] shouldBe thrownBy(subscribeAggregatedHealthCallback(SubsystemKey(LGSF), println).ready().await)
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribe aggregated health via actorRef for a subsystem") {
    getAggregatedHealth(GlobalKey).await shouldBe Bad
    getCurrentSeverity(cpuExceededAlarmKey).await shouldBe Disconnected
    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected

    // subsystem subscription - tcs
    val testProbe         = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription = subscribeAggregatedHealthActorRef(SubsystemKey(TCS), testProbe.ref)
    alarmSubscription.ready().await
    testProbe.expectMessage(Bad) // on subscription, current aggregated health will be calculated

    setSeverity(cpuExceededAlarmKey, Okay).await
    testProbe.expectNoMessage(100.millis) // outOfRangeOffloadAlarmKey is still disconnected, hence aggregated heath = Bad which means no change

    setSeverity(outOfRangeOffloadAlarmKey, Okay).await
    testProbe.expectMessage(Good)

    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await
    testProbe.expectNoMessage(200.millis)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("shelved alarms should be considered in health aggregation") {
    getAggregatedHealth(GlobalKey).await shouldBe Bad
    getCurrentSeverity(cpuExceededAlarmKey).await shouldBe Disconnected

    val testProbe         = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription = subscribeAggregatedHealthActorRef(SubsystemKey(TCS), testProbe.ref)
    alarmSubscription.ready().await
    testProbe.expectMessage(Bad) // on subscription, current aggregated health will be calculated

    // initialize alarm with Shelved status just for this test
    shelve(cpuExceededAlarmKey).await
    getStatus(cpuExceededAlarmKey).await.shelveStatus shouldBe Shelved

    setSeverity(cpuExceededAlarmKey, Okay).await
    testProbe.expectNoMessage(100.millis) // outOfRangeOffloadAlarmKey is still disconnected, hence aggregated heath = Bad which means no change

    setSeverity(outOfRangeOffloadAlarmKey, Okay).await
    testProbe.expectMessage(Good)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribeAggregatedHealthActorRef should throw InactiveAlarmException when all resolved keys are inactive") {
    a[InactiveAlarmException] shouldBe thrownBy {
      val testProbe    = TestProbe[AlarmHealth]()(actorSystem.toTyped)
      val subscription = subscribeAggregatedHealthActorRef(SubsystemKey(LGSF), testProbe.ref)
      subscription.ready().await
    }
  }
}
