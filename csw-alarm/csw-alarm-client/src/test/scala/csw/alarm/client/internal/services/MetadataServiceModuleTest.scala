package csw.alarm.client.internal.services

import com.typesafe.config.{Config, ConfigFactory}
import csw.alarm.api.exceptions.KeyNotFoundException
import csw.alarm.api.internal.MetadataKey
import csw.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.alarm.models.ActivationStatus.{Active, Inactive}
import csw.alarm.models.AlarmHealth.Bad
import csw.alarm.models.AlarmSeverity._
import csw.alarm.models.FullAlarmSeverity.Disconnected
import csw.alarm.models.Key.{AlarmKey, ComponentKey, GlobalKey, SubsystemKey}
import csw.alarm.models.{ActivationStatus, AlarmStatus}
import csw.prefix.models.{Prefix, Subsystem}
import csw.prefix.models.Subsystem.{CSW, NFIRAOS}

// DEOPSCSW-486: Provide API to load alarm metadata in Alarm store from file
// CSW-83: Alarm models should take prefix
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
  test("getMetadata should fetch metadata of the given Alarm key | DEOPSCSW-486, DEOPSCSW-445") {
    initTestAlarms()
    getMetadata(tromboneAxisHighLimitAlarmKey).await shouldBe tromboneAxisHighLimitAlarm
    getMetadata(splitterLimitAlarmKey).await shouldBe splitterLimitAlarm
  }

  //  DEOPSCSW-445: Get api for alarm metadata
  test("getMetadata should throw exception while getting metadata if key does not exist | DEOPSCSW-486, DEOPSCSW-445") {
    val invalidAlarm = AlarmKey(Prefix(CSW, "invalid"), "invalid")
    an[KeyNotFoundException] shouldBe thrownBy(getMetadata(invalidAlarm).await)
  }

  // DEOPSCSW-463: Fetch Alarm List for a component name or pattern
  test("getMetadata should fetch all alarms for a component | DEOPSCSW-486, DEOPSCSW-463") {
    initTestAlarms()
    val tromboneKey      = ComponentKey(Prefix(NFIRAOS, "trombone"))
    val tromboneMetadata = getMetadata(tromboneKey).await
    tromboneMetadata.length shouldBe 2
    tromboneMetadata should contain allElementsOf List(tromboneAxisLowLimitAlarm, tromboneAxisHighLimitAlarm)

    val enclosureKey      = ComponentKey(Prefix(NFIRAOS, "enclosure"))
    val enclosureMetadata = getMetadata(enclosureKey).await
    enclosureMetadata.length shouldBe 2
    enclosureMetadata should contain allElementsOf List(enclosureTempHighAlarm, enclosureTempLowAlarm)
  }

  // DEOPSCSW-464: Fetch Alarm name list for a subsystem name or pattern
  test("getMetadata should fetch all alarms for a subsystem | DEOPSCSW-486, DEOPSCSW-464") {
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
  test("getMetadata should fetch all alarms of whole system | DEOPSCSW-486, DEOPSCSW-464") {
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
  test(
    "getMetadata should throw exception if no alarms are found while getting metadata by subsystem | DEOPSCSW-486, DEOPSCSW-464"
  ) {
    an[KeyNotFoundException] shouldBe thrownBy(getMetadata(SubsystemKey(Subsystem.CSW)).await)
  }

  val fourAlarmsConfig: Config = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
  val twoAlarmsConfig: Config  = ConfigFactory.parseResources("test-alarms/two-valid-alarms.conf")

  test("initAlarms should load alarms from provided config file | DEOPSCSW-486") {
    clearAlarmStore().await
    a[KeyNotFoundException] shouldBe thrownBy(getMetadata(GlobalKey).await)

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

  test("initAlarms should reset the previous alarm data in redis and load with newly provided | DEOPSCSW-486") {
    clearAlarmStore().await
    a[KeyNotFoundException] shouldBe thrownBy(getMetadata(GlobalKey).await)

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

  test("initAlarm with reset should not delete keys other than alarm service, for example sentinel related keys | DEOPSCSW-486") {
    clearAlarmStore().await
    a[KeyNotFoundException] shouldBe thrownBy(getMetadata(GlobalKey).await)

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

  test("initAlarm with reset=false should preserve existing alarm keys | DEOPSCSW-486") {
    clearAlarmStore().await
    a[KeyNotFoundException] shouldBe thrownBy(getMetadata(GlobalKey).await)

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

  // DEOPSCSW-443: Model to represent Alarm Activation status
  // DEOPSCSW-448: Set Activation status for an alarm entity
  test("activate should activate an inactive alarm | DEOPSCSW-486, DEOPSCSW-443, DEOPSCSW-448") {
    initTestAlarms()

    // ensure alarm is Inactive first
    getMetadata(cpuIdleAlarmKey).await.activationStatus should be(ActivationStatus.Inactive)

    activate(cpuIdleAlarmKey).await

    getMetadata(cpuIdleAlarmKey).await.activationStatus shouldBe Active
  }

  // DEOPSCSW-443: Model to represent Alarm Activation status
  // DEOPSCSW-448: Set Activation status for an alarm entity
  test("deActivate should deactivate an active alarm | DEOPSCSW-486, DEOPSCSW-443, DEOPSCSW-448") {
    initTestAlarms()

    // ensure alarm is Active first
    getMetadata(outOfRangeOffloadAlarmKey).await.activationStatus should be(ActivationStatus.Active)

    deactivate(outOfRangeOffloadAlarmKey).await

    getMetadata(outOfRangeOffloadAlarmKey).await.activationStatus shouldBe Inactive
  }

  // DEOPSCSW-443: Model to represent Alarm Activation status
  // DEOPSCSW-448: Set Activation status for an alarm entity
  test(
    "should throw exception when tried to activate/deactivate alarm which is not present in alarm store | DEOPSCSW-486, DEOPSCSW-443, DEOPSCSW-448"
  ) {
    val invalidKey = AlarmKey(Prefix(CSW, "invalid"), "invalid")

    an[KeyNotFoundException] shouldBe thrownBy(activate(invalidKey).await)
    an[KeyNotFoundException] shouldBe thrownBy(deactivate(invalidKey).await)
  }
}
