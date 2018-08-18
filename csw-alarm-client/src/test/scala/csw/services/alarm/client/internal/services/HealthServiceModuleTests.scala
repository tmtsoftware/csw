package csw.services.alarm.client.internal.services

import com.typesafe.config.ConfigFactory
import csw.messages.params.models.Subsystem.{BAD, LGSF, NFIRAOS}
import csw.services.alarm.api.exceptions.{InactiveAlarmException, KeyNotFoundException}
import csw.services.alarm.api.models.AlarmHealth.{Bad, Good, Ill}
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.Key.{ComponentKey, GlobalKey, SubsystemKey}
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture

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
}
