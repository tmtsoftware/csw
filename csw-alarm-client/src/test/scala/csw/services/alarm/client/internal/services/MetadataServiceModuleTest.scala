package csw.services.alarm.client.internal.services

import com.typesafe.config.{Config, ConfigFactory}
import csw.messages.params.models.Subsystem
import csw.messages.params.models.Subsystem.NFIRAOS
import csw.services.alarm.api.exceptions.KeyNotFoundException
import csw.services.alarm.api.internal.MetadataKey
import csw.services.alarm.api.models.ActivationStatus.{Active, Inactive}
import csw.services.alarm.api.models.AlarmHealth.Bad
import csw.services.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.{ActivationStatus, AlarmStatus}
import csw.services.alarm.api.models.Key.{AlarmKey, ComponentKey, GlobalKey, SubsystemKey}
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture

// DEOPSCSW-486: Provide API to load alarm metadata in Alarm store from file
class MetadataServiceModuleTest
    extends AlarmServiceTestSetup
    with MetadataServiceModule
    with SeverityServiceModule
    with StatusServiceModule
    with HealthServiceModule {

  def initTestAlarms(): Unit = {
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/more-alarms.conf")
    initAlarms(validAlarmsConfig, reset = true).await
  }

  //  DEOPSCSW-445: Get api for alarm metadata
  test("getMetadata should fetch metadata of the given Alarm key") {
    initTestAlarms()
    getMetadata(tromboneAxisHighLimitAlarmKey).await shouldBe tromboneAxisHighLimitAlarm
    getMetadata(splitterLimitAlarmKey).await shouldBe splitterLimitAlarm
  }

  //  DEOPSCSW-445: Get api for alarm metadata
  test("getMetadata should throw exception while getting metadata if key does not exist") {
    val invalidAlarm = AlarmKey(Subsystem.BAD, "invalid", "invalid")
    an[KeyNotFoundException] shouldBe thrownBy(getMetadata(invalidAlarm).await)
  }

  // DEOPSCSW-463: Fetch Alarm List for a component name or pattern
  test("getMetadata should fetch all alarms for a component") {
    initTestAlarms()
    val tromboneKey      = ComponentKey(NFIRAOS, "trombone")
    val tromboneMetadata = getMetadata(tromboneKey).await
    tromboneMetadata.length shouldBe 2
    tromboneMetadata should contain allElementsOf List(tromboneAxisLowLimitAlarm, tromboneAxisHighLimitAlarm)

    val enclosureKey      = ComponentKey(NFIRAOS, "enclosure")
    val enclosureMetadata = getMetadata(enclosureKey).await
    enclosureMetadata.length shouldBe 2
    enclosureMetadata should contain allElementsOf List(enclosureTempHighAlarm, enclosureTempLowAlarm)
  }

  // DEOPSCSW-464: Fetch Alarm name list for a subsystem name or pattern
  test("getMetadata should fetch all alarms for a subsystem") {
    initTestAlarms()
    val nfiraosKey = SubsystemKey(NFIRAOS)
    val metadata   = getMetadata(nfiraosKey).await
    metadata.length shouldBe 5
    metadata should contain allElementsOf List(
      splitterLimitAlarm,
      tromboneAxisHighLimitAlarm,
      tromboneAxisLowLimitAlarm,
      enclosureTempHighAlarm,
      enclosureTempLowAlarm
    )
  }

  // DEOPSCSW-464: Fetch Alarm name list for a subsystem name or pattern
  test("getMetadata should fetch all alarms of whole system") {
    initTestAlarms()
    val globalKey = GlobalKey
    val metadata  = getMetadata(globalKey).await
    metadata.length shouldBe 8
    metadata should contain allElementsOf List(
      tromboneAxisHighLimitAlarm,
      tromboneAxisLowLimitAlarm,
      splitterLimitAlarm,
      enclosureTempHighAlarm,
      enclosureTempLowAlarm,
      cpuExceededAlarm,
      outOfRangeOffloadAlarm,
      cpuIdleAlarm
    )
  }

  // DEOPSCSW-464: Fetch Alarm name list for a subsystem name or pattern
  test("getMetadata should throw exception if no alarms are found while getting metadata by subsystem") {
    an[KeyNotFoundException] shouldBe thrownBy(getMetadata(SubsystemKey(Subsystem.BAD)).await)
  }

  val fourAlarmsConfig: Config = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
  val twoAlarmsConfig: Config  = ConfigFactory.parseResources("test-alarms/two-valid-alarms.conf")

  test("initAlarms should load alarms from provided config file") {
    initTestAlarms()

    // valid-alarms.conf contains 4 alarms
    getMetadata(GlobalKey).await.size shouldBe 8

    getMetadata(tromboneAxisHighLimitAlarmKey).await shouldBe tromboneAxisHighLimitAlarm
    val alarmStatus = getStatus(tromboneAxisHighLimitAlarmKey).await
    alarmStatus shouldBe AlarmStatus().copy(alarmTime = alarmStatus.alarmTime)
    // Severity does not get loaded in alarm store on init, but it gets interpreted as Disconnected by getSeverity API
    testSeverityApi.get(tromboneAxisHighLimitAlarmKey).await shouldBe None
    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected
    getAggregatedHealth(tromboneAxisHighLimitAlarmKey).await shouldBe Bad
  }

  test("initAlarms should reset the previous alarm data in redis and load with newly provided") {
    // valid-alarms.conf contains 4 alarms, cpuExceededAlarm is one of them
    initAlarms(fourAlarmsConfig, reset = true).await
    getMetadata(GlobalKey).await.size shouldBe 4
    getMetadata(cpuExceededAlarmKey).await shouldBe cpuExceededAlarm
    getMetadata(tromboneAxisHighLimitAlarmKey).await shouldBe tromboneAxisHighLimitAlarm

    // update tromboneAxisHighLimitAlarmKey
    setSeverity(tromboneAxisHighLimitAlarmKey, Major).await

    // two-valid-alarms.conf contains 2 alarms, it does not contain cpuExceededAlarm
    initAlarms(twoAlarmsConfig, reset = true).await
    getMetadata(GlobalKey).await.size shouldBe 2
    an[KeyNotFoundException] shouldBe thrownBy(getMetadata(cpuExceededAlarmKey).await)

    // tromboneAxisHighLimitAlarm is present in both the files and hence rewritten in second load
    getMetadata(tromboneAxisHighLimitAlarmKey).await shouldBe tromboneAxisHighLimitAlarm
    val alarmStatus = getStatus(tromboneAxisHighLimitAlarmKey).await
    alarmStatus shouldEqual AlarmStatus().copy(alarmTime = alarmStatus.alarmTime)
    getCurrentSeverity(tromboneAxisHighLimitAlarmKey).await shouldBe Disconnected
  }

  test("initAlarm with reset should not delete keys other than alarm service, for example sentinel related keys") {
    // two-valid-alarms.conf contains 2 alarms
    initAlarms(twoAlarmsConfig, reset = true).await
    getMetadata(GlobalKey).await.size shouldBe 2

    // bypassing AlarmService, set some value for MetadataKey using RedisAsyncApi in order to simulate keys other than alarm service
    testMetadataApi.set(MetadataKey("sentinel.a.b.c"), cpuExceededAlarm).await
    testMetadataApi.get(MetadataKey("sentinel.a.b.c")).await shouldBe Some(cpuExceededAlarm)

    initAlarms(twoAlarmsConfig, reset = true).await
    // alarm service related keys will still be 2 but there should be one additional key [sentinel.a.b.c] other than alarm service
    getMetadata(GlobalKey).await.size shouldBe 2
    testMetadataApi.get(MetadataKey("sentinel.a.b.c")).await shouldBe Some(cpuExceededAlarm)
  }

  test("initAlarm with reset=false should preserve existing alarm keys") {
    // cpuExceededAlarm present in this file
    initAlarms(fourAlarmsConfig, reset = true).await
    getMetadata(GlobalKey).await.size shouldBe 4
    getMetadata(cpuExceededAlarmKey).await shouldBe cpuExceededAlarm

    // update tromboneAxisLowLimitAlarmKey
    setSeverity(tromboneAxisLowLimitAlarmKey, Critical).await

    // cpuExceededAlarm does not present in this file, but reset=false, it is preserved
    initAlarms(twoAlarmsConfig).await
    getMetadata(GlobalKey).await.size shouldBe 4
    getMetadata(cpuExceededAlarmKey).await shouldBe cpuExceededAlarm

    // current severity will be expired at it's time and will be inferred disconnected
    val alarmStatus = getStatus(tromboneAxisLowLimitAlarmKey).await
    alarmStatus shouldEqual AlarmStatus().copy(alarmTime = alarmStatus.alarmTime)
  }

  // DEOPSCSW-448: Set Activation status for an alarm entity
  test("activate should activate an inactive alarm") {
    initTestAlarms()

    //ensure alarm is Inactive first
    getMetadata(cpuIdleAlarmKey).await.activationStatus should be(ActivationStatus.Inactive)

    activate(cpuIdleAlarmKey).await

    getMetadata(cpuIdleAlarmKey).await.activationStatus shouldBe Active
  }

  // DEOPSCSW-448: Set Activation status for an alarm entity
  test("deActivate should deactivate an active alarm") {
    initTestAlarms()

    //ensure alarm is Active first
    getMetadata(outOfRangeOffloadAlarmKey).await.activationStatus should be(ActivationStatus.Active)

    deactivate(outOfRangeOffloadAlarmKey).await

    getMetadata(outOfRangeOffloadAlarmKey).await.activationStatus shouldBe Inactive
  }

  // DEOPSCSW-448: Set Activation status for an alarm entity
  test("should throw exception when tried to activate/deactivate alarm which is not present in alarm store") {
    val invalidKey = AlarmKey(Subsystem.BAD, "invalid", "invalid")

    an[KeyNotFoundException] shouldBe thrownBy(activate(invalidKey).await)
    an[KeyNotFoundException] shouldBe thrownBy(deactivate(invalidKey).await)
  }
}
