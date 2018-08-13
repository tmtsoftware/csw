package csw.services.alarm.client.internal.services

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.services.alarm.api.exceptions.{InactiveAlarmException, KeyNotFoundException}
import csw.services.alarm.api.models.AlarmHealth.Bad
import csw.services.alarm.api.models.AlarmSeverity.{Critical, Okay}
import csw.services.alarm.api.models.Key
import csw.services.alarm.client.internal.commons.Settings
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory

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
  test("getAggregatedHealth should should get aggregated severity for a subsystem") {
    setSeverity(tromboneAxisHighLimitAlarmKey, Okay).await
    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    val tromboneKey = Key.SubsystemKey("nfiraos")
    getAggregatedHealth(tromboneKey).await shouldBe Bad
  }

  // DEOPSCSW-466: Fetch alarm severity, component or subsystem
  test("getAggregatedHealth should throw KeyNotFoundException when key is invalid") {
    val invalidAlarm = Key.SubsystemKey("invalid")
    intercept[KeyNotFoundException] {
      getAggregatedHealth(invalidAlarm).await
    }
  }

  // DEOPSCSW-466: Fetch alarm severity, component or subsystem
  test("getAggregatedHealth should throw InactiveAlarmException when all resolved keys are inactive") {
    val invalidAlarm = Key.SubsystemKey("LGSF")
    intercept[InactiveAlarmException] {
      getAggregatedHealth(invalidAlarm).await
    }
  }
}
