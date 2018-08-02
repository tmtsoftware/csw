package csw.services.alarm.client.internal
import java.io.File

import csw.services.alarm.api.exceptions.KeyNotFoundException
import csw.services.alarm.api.internal.MetadataKey
import csw.services.alarm.api.models.ActivationStatus.Active
import csw.services.alarm.api.models.AlarmHealth.Bad
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.AlarmType.Absolute
import csw.services.alarm.api.models.Key.{AlarmKey, GlobalKey}
import csw.services.alarm.api.models.{AlarmMetadata, AlarmStatus, AlarmType}
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture

// DEOPSCSW-486: Provide API to load alarm metadata in Alarm store from file
class InitAlarmStoreTest extends AlarmServiceTestSetup(2639, 6379) {

  val nfiraosAlarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
  private val tromboneAxisHighLimitAlarm = AlarmMetadata(
    subsystem = "nfiraos",
    component = "trombone",
    name = "tromboneAxisHighLimitAlarm",
    description = "Warns when trombone axis has reached the high limit",
    location = "south side",
    AlarmType.Absolute,
    Set(Indeterminate, Okay, Warning, Major),
    probableCause = "the trombone software has failed or the stage was driven into the high limit",
    operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command",
    isAutoAcknowledgeable = true,
    isLatchable = true,
    activationStatus = Active
  )

  val tcpAlarmKey = AlarmKey("TCS", "tcsPk", "cpuExceededAlarm")
  private val cpuExceededAlarm = AlarmMetadata(
    subsystem = "TCS",
    component = "tcsPk",
    name = "cpuExceededAlarm",
    description =
      "This alarm is activated when the tcsPk Assembly can no longer calculate all of its pointing values in the time allocated. The CPU may lock power, or there may be pointing loops running that are not needed. Response: Check to see if pointing loops are executing that are not needed or see about a more powerful CPU.",
    location = "in computer...",
    alarmType = Absolute,
    supportedSeverities = Set(Indeterminate, Okay, Warning, Major, Critical),
    probableCause = "too fast...",
    operatorResponse = "slow it down...",
    isAutoAcknowledgeable = true,
    isLatchable = false,
    activationStatus = Active
  )

  val threeAlarmConfFile = new File(getClass.getResource("/test-alarms/valid-alarms.conf").getPath)
  val twoAlarmConfFile   = new File(getClass.getResource("/test-alarms/two-valid-alarms.conf").getPath)

  test("should load alarms from provided config file") {
    alarmService.initAlarms(threeAlarmConfFile).await

    // valid-alarms.conf contains 3 alarms
    alarmService.getMetadata(GlobalKey).await.size shouldBe 3

    alarmService.getMetadata(nfiraosAlarmKey).await shouldBe tromboneAxisHighLimitAlarm
    alarmService.getStatus(nfiraosAlarmKey).await shouldBe AlarmStatus()
    // Severity does not get loaded in alarm store on init, but it gets interpreted as Disconnected by getSeverity API
    alarmServiceFactory.severityApi.get(nfiraosAlarmKey).await shouldBe None
    alarmService.getSeverity(nfiraosAlarmKey).await shouldBe Disconnected
    alarmService.getAggregatedHealth(nfiraosAlarmKey).await shouldBe Bad
  }

  test("should reset the previous alarm data in redis and load with newly provided") {
    // valid-alarms.conf contains 3 alarms, cpuExceededAlarm is one of them
    alarmService.initAlarms(threeAlarmConfFile, reset = true).await
    alarmService.getMetadata(GlobalKey).await.size shouldBe 3
    alarmService.getMetadata(tcpAlarmKey).await shouldBe cpuExceededAlarm
    alarmService.getMetadata(nfiraosAlarmKey).await shouldBe tromboneAxisHighLimitAlarm

    // two-valid-alarms.conf contains 2 alarms, it does not contain cpuExceededAlarm
    alarmService.initAlarms(twoAlarmConfFile, reset = true).await
    alarmService.getMetadata(GlobalKey).await.size shouldBe 2
    intercept[KeyNotFoundException] {
      alarmService.getMetadata(tcpAlarmKey).await
    }
    // tromboneAxisHighLimitAlarm is present in both the files and hence rewritten in second load
    alarmService.getMetadata(nfiraosAlarmKey).await shouldBe tromboneAxisHighLimitAlarm
  }

  test("initAlarm with reset should not delete keys other than alarm service, for example sentinel related keys") {
    // two-valid-alarms.conf contains 2 alarms
    alarmService.initAlarms(twoAlarmConfFile, reset = true).await
    alarmService.getMetadata(GlobalKey).await.size shouldBe 2

    // bypassing AlarmService, set some value for MetadataKey using RedisAsyncScalaApi in order to simulate keys other than alarm service
    alarmServiceFactory.metatdataApi.set(MetadataKey("sentinel.a.b.c"), cpuExceededAlarm).await
    alarmServiceFactory.metatdataApi.get(MetadataKey("sentinel.a.b.c")).await shouldBe Some(cpuExceededAlarm)

    alarmService.initAlarms(twoAlarmConfFile, reset = true).await
    // alarm service related keys will still be 2 but there should be one additional key [sentinel.a.b.c] other than alarm service
    alarmService.getMetadata(GlobalKey).await.size shouldBe 2
    alarmServiceFactory.metatdataApi.get(MetadataKey("sentinel.a.b.c")).await shouldBe Some(cpuExceededAlarm)
  }

  test("initAlarm with reset=false should preserve existing alarm keys") {
    // cpuExceededAlarm present in this file
    alarmService.initAlarms(threeAlarmConfFile, reset = true).await
    alarmService.getMetadata(GlobalKey).await.size shouldBe 3
    alarmService.getMetadata(tcpAlarmKey).await shouldBe cpuExceededAlarm

    // cpuExceededAlarm does not present in this file, but reset=false, it is preserved
    alarmService.initAlarms(twoAlarmConfFile).await
    alarmService.getMetadata(GlobalKey).await.size shouldBe 3
    alarmService.getMetadata(tcpAlarmKey).await shouldBe cpuExceededAlarm
  }
}
