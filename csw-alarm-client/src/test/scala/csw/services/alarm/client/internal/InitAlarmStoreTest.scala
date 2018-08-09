package csw.services.alarm.client.internal
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.alarm.api.exceptions.KeyNotFoundException
import csw.services.alarm.api.internal.MetadataKey
import csw.services.alarm.api.models.AlarmHealth.Bad
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.AlarmStatus
import csw.services.alarm.api.models.Key.GlobalKey
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture

// DEOPSCSW-486: Provide API to load alarm metadata in Alarm store from file
class InitAlarmStoreTest extends AlarmServiceTestSetup {

  val fourAlarmsConfig: Config = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
  val twoAlarmsConfig: Config  = ConfigFactory.parseResources("test-alarms/two-valid-alarms.conf")

  test("should load alarms from provided config file") {
    alarmService.initAlarms(fourAlarmsConfig).await

    // valid-alarms.conf contains 4 alarms
    alarmService.getMetadata(GlobalKey).await.size shouldBe 4

    alarmService.getMetadata(tromboneAxisHighLimitAlarmKey).await shouldBe tromboneAxisHighLimitAlarm
    alarmService.getStatus(tromboneAxisHighLimitAlarmKey).await shouldBe AlarmStatus()
    // Severity does not get loaded in alarm store on init, but it gets interpreted as Disconnected by getSeverity API
    testSeverityApi.get(tromboneAxisHighLimitAlarmKey).await shouldBe None
    alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected
    alarmService.getAggregatedHealth(tromboneAxisHighLimitAlarmKey).await shouldBe Bad
  }

  test("should reset the previous alarm data in redis and load with newly provided") {
    // valid-alarms.conf contains 4 alarms, cpuExceededAlarm is one of them
    alarmService.initAlarms(fourAlarmsConfig, reset = true).await
    alarmService.getMetadata(GlobalKey).await.size shouldBe 4
    alarmService.getMetadata(cpuExceededAlarmKey).await shouldBe cpuExceededAlarm
    alarmService.getMetadata(tromboneAxisHighLimitAlarmKey).await shouldBe tromboneAxisHighLimitAlarm

    // update tromboneAxisHighLimitAlarmKey
    alarmService.setSeverity(tromboneAxisHighLimitAlarmKey, Major).await

    // two-valid-alarms.conf contains 2 alarms, it does not contain cpuExceededAlarm
    alarmService.initAlarms(twoAlarmsConfig, reset = true).await
    alarmService.getMetadata(GlobalKey).await.size shouldBe 2
    intercept[KeyNotFoundException] {
      alarmService.getMetadata(cpuExceededAlarmKey).await
    }

    // tromboneAxisHighLimitAlarm is present in both the files and hence rewritten in second load
    alarmService.getMetadata(tromboneAxisHighLimitAlarmKey).await shouldBe tromboneAxisHighLimitAlarm
    alarmService.getStatus(tromboneAxisHighLimitAlarmKey).await shouldEqual AlarmStatus()
    alarmService.getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected
  }

  test("initAlarm with reset should not delete keys other than alarm service, for example sentinel related keys") {
    // two-valid-alarms.conf contains 2 alarms
    alarmService.initAlarms(twoAlarmsConfig, reset = true).await
    alarmService.getMetadata(GlobalKey).await.size shouldBe 2

    // bypassing AlarmService, set some value for MetadataKey using RedisAsyncScalaApi in order to simulate keys other than alarm service
    testMetadataApi.set(MetadataKey("sentinel.a.b.c"), cpuExceededAlarm).await
    testMetadataApi.get(MetadataKey("sentinel.a.b.c")).await shouldBe Some(cpuExceededAlarm)

    alarmService.initAlarms(twoAlarmsConfig, reset = true).await
    // alarm service related keys will still be 2 but there should be one additional key [sentinel.a.b.c] other than alarm service
    alarmService.getMetadata(GlobalKey).await.size shouldBe 2
    testMetadataApi.get(MetadataKey("sentinel.a.b.c")).await shouldBe Some(cpuExceededAlarm)
  }

  test("initAlarm with reset=false should preserve existing alarm keys") {
    // cpuExceededAlarm present in this file
    alarmService.initAlarms(fourAlarmsConfig, reset = true).await
    alarmService.getMetadata(GlobalKey).await.size shouldBe 4
    alarmService.getMetadata(cpuExceededAlarmKey).await shouldBe cpuExceededAlarm

    // update tromboneAxisLowLimitAlarmKey
    alarmService.setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    // cpuExceededAlarm does not present in this file, but reset=false, it is preserved
    alarmService.initAlarms(twoAlarmsConfig).await
    alarmService.getMetadata(GlobalKey).await.size shouldBe 4
    alarmService.getMetadata(cpuExceededAlarmKey).await shouldBe cpuExceededAlarm

    // current severity will be expired at it's time and will be inferred disconnected
    alarmService.getStatus(tromboneAxisLowLimitAlarmKey).await shouldEqual AlarmStatus()
  }
}
