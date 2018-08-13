package csw.services.alarm.client.internal.services

import com.typesafe.config.ConfigFactory
import csw.services.alarm.api.exceptions.KeyNotFoundException
import csw.services.alarm.api.models.Key.{AlarmKey, ComponentKey, GlobalKey, SubsystemKey}
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture

class MetadataServiceModuleTests extends AlarmServiceTestSetup {

  override protected def beforeEach(): Unit = {
    val validAlarmsConfig = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
    alarmService.initAlarms(validAlarmsConfig, reset = true).await
  }

  //  DEOPSCSW-445: Get api for alarm metadata
  test("getMetadata should fetch metadata of the given Alarm key") {
    alarmService.getMetadata(tromboneAxisHighLimitAlarmKey).await shouldBe tromboneAxisHighLimitAlarm
  }

  //  DEOPSCSW-445: Get api for alarm metadata
  test("getMetadata should throw exception while getting metadata if key does not exist") {
    val invalidAlarm = AlarmKey("invalid", "invalid", "invalid")
    intercept[KeyNotFoundException] {
      alarmService.getMetadata(invalidAlarm).await
    }
  }

  // DEOPSCSW-463: Fetch Alarm List for a component name or pattern
  test("getMetadata should fetch all alarms for a component") {
    val tromboneKey = ComponentKey("TCS", "tcsPk")
    alarmService.getMetadata(tromboneKey).await should contain allElementsOf List(cpuExceededAlarm)
  }

  // DEOPSCSW-464: Fetch Alarm name list for a subsystem name or pattern
  test("getMetadata should fetch all alarms for a subsystem") {
    val nfiraosKey = SubsystemKey("nfiraos")
    alarmService.getMetadata(nfiraosKey).await should contain allElementsOf List(
      tromboneAxisHighLimitAlarm,
      tromboneAxisLowLimitAlarm
    )
  }

  // DEOPSCSW-464: Fetch Alarm name list for a subsystem name or pattern
  test("getMetadata should fetch all alarms of whole system") {
    val globalKey = GlobalKey
    alarmService.getMetadata(globalKey).await should contain allElementsOf List(
      tromboneAxisHighLimitAlarm,
      tromboneAxisLowLimitAlarm,
      cpuExceededAlarm
    )
  }

  // DEOPSCSW-464: Fetch Alarm name list for a subsystem name or pattern
  test("getMetadata should throw exception if no alarms are found while getting metadata by subsystem") {
    val invalidAlarm = SubsystemKey("invalid")
    intercept[KeyNotFoundException] {
      alarmService.getMetadata(invalidAlarm).await
    }
  }
}
