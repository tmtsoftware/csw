package csw.services.alarm.client.internal.services

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import com.typesafe.config.ConfigFactory
import csw.messages.params.models.Subsystem.{BAD, LGSF, NFIRAOS, TCS}
import csw.services.alarm.api.exceptions.{InactiveAlarmException, KeyNotFoundException}
import csw.services.alarm.api.models.{AlarmHealth, AlarmStatus}
import csw.services.alarm.api.models.AlarmHealth.{Bad, Good, Ill}
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.Key.{ComponentKey, GlobalKey, SubsystemKey}
import csw.services.alarm.api.models.ShelveStatus.Shelved
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture

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
    setSeverity(tromboneAxisHighLimitAlarmKey, Okay).await
    getAggregatedHealth(tromboneAxisHighLimitAlarmKey).await shouldBe Good

    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await
    getAggregatedHealth(tromboneAxisHighLimitAlarmKey).await shouldBe Ill

    setSeverity(tromboneAxisHighLimitAlarmKey, Indeterminate).await
    getAggregatedHealth(tromboneAxisHighLimitAlarmKey).await shouldBe Bad
  }

  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should get aggregated health for a component") {
    setSeverity(tromboneAxisHighLimitAlarmKey, Okay).await
    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    val tromboneKey = ComponentKey(NFIRAOS, "trombone")
    getAggregatedHealth(tromboneKey).await shouldBe Bad
  }

  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should get aggregated health for a subsystem") {
    setSeverity(tromboneAxisHighLimitAlarmKey, Okay).await
    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    val tromboneKey = SubsystemKey(NFIRAOS)
    getAggregatedHealth(tromboneKey).await shouldBe Bad
  }

  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should get aggregated health for global system") {
    setSeverity(tromboneAxisHighLimitAlarmKey, Okay).await
    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    getAggregatedHealth(GlobalKey).await shouldBe Bad
  }

  // DEOPSCSW-448: Set Activation status for an alarm entity
  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should not consider inactive alarms for health aggregation") {
    setSeverity(enclosureTempHighAlarmKey, Okay).await
    setSeverity(enclosureTempLowAlarmKey, Critical).await

    val tromboneKey = ComponentKey(NFIRAOS, "enclosure")
    getAggregatedHealth(tromboneKey).await shouldBe Good
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  // DEOPSCSW-466: Fetch health for a given alarm, component name or a subsystem name
  test("getAggregatedHealth should consider shelved alarms also for health aggregation") {
    setSeverity(cpuExceededAlarmKey, Okay).await
    setSeverity(cpuExceededAlarmKey, Critical).await

    val tromboneKey = ComponentKey(cpuExceededAlarmKey.subsystem, cpuExceededAlarmKey.component)
    getAggregatedHealth(tromboneKey).await shouldBe Bad
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
    val testProbe         = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription = subscribeAggregatedHealthCallback(tromboneAxisLowLimitAlarmKey, testProbe.ref ! _)
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
    val testProbe         = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription = subscribeAggregatedHealthCallback(SubsystemKey(TCS), testProbe.ref ! _)
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

  // DEOPSCSW-448: Set Activation status for an alarm entity
  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribeAggregatedHealthCallback should not consider inactive alarm for aggregation") {

    val testProbe = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription =
      subscribeAggregatedHealthCallback(ComponentKey(NFIRAOS, "enclosure"), testProbe.ref ! _)
    alarmSubscription.ready().await

    Thread.sleep(500) // wait for redis connection to happen
    enclosureTempHighAlarm.isActive shouldBe true
    setSeverity(enclosureTempHighAlarmKey, Okay).await

    testProbe.expectMessage(Good)

    setSeverity(enclosureTempHighAlarmKey, Okay).await

    enclosureTempLowAlarm.isActive shouldBe false
    setSeverity(enclosureTempLowAlarmKey, Critical).await

    testProbe.expectMessage(Good)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribeAggregatedSeverityCallback should throw KeyNotFoundException if the key does not match any key") {
    an[KeyNotFoundException] shouldBe thrownBy {
      val subscription = subscribeAggregatedHealthCallback(SubsystemKey(BAD), println)
      subscription.ready().await
    }
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribeAggregatedHealthCallback should throw KeyNotFoundException when key is invalid") {
    an[KeyNotFoundException] shouldBe thrownBy {
      val subscription = subscribeAggregatedHealthCallback(SubsystemKey(BAD), println)
      subscription.ready().await
    }
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribeAggregatedHealthCallback should throw InactiveAlarmException when all resolved keys are inactive") {
    an[InactiveAlarmException] shouldBe thrownBy {
      val subscription = subscribeAggregatedHealthCallback(SubsystemKey(LGSF), println)
      subscription.ready().await
    }
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribe aggregated health via actorRef for a subsystem") {

    getAggregatedHealth(GlobalKey).await shouldBe Bad

    // subsystem subscription - tcs
    val testProbe         = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription = subscribeAggregatedHealthActorRef(SubsystemKey(TCS), testProbe.ref)
    alarmSubscription.ready().await

    Thread.sleep(500) // wait for redis connection to happen

    setSeverity(cpuExceededAlarmKey, Okay).await

    testProbe.expectMessage(Good)
    testProbe.expectMessage(Bad) // severity expires after 1 second in test

    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await
    testProbe.expectNoMessage(200.millis)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
  test("shelved alarms should be considered in health aggregation") {

    val testProbe         = TestProbe[AlarmHealth]()(actorSystem.toTyped)
    val alarmSubscription = subscribeAggregatedHealthActorRef(SubsystemKey(TCS), testProbe.ref)
    alarmSubscription.ready().await

    Thread.sleep(500) // wait for redis connection to happen

    setStatus(cpuExceededAlarmKey, AlarmStatus().copy(shelveStatus = Shelved)) //shelve the alarm

    setSeverity(cpuExceededAlarmKey, Critical).await
    testProbe.expectMessage(Bad)

    setSeverity(cpuExceededAlarmKey, Okay).await
    testProbe.expectMessage(Good)

    alarmSubscription.unsubscribe().await
  }

  // DEOPSCSW-468: Monitor health values based on alarm severities for a single alarm, component, subsystem or all
  test("subscribeAggregatedHealthActorRef should throw InactiveAlarmException when all resolved keys are inactive") {
    an[InactiveAlarmException] shouldBe thrownBy {
      val testProbe    = TestProbe[AlarmHealth]()(actorSystem.toTyped)
      val subscription = subscribeAggregatedHealthActorRef(SubsystemKey(LGSF), testProbe.ref)
      subscription.ready().await
    }
  }
}
