package csw.services.alarm.client.internal
import java.io.File

import csw.services.alarm.api.exceptions.KeyNotFoundException
import csw.services.alarm.api.models.ActivationStatus.Active
import csw.services.alarm.api.models.AlarmHealth.Bad
import csw.services.alarm.api.models.AlarmSeverity._
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{AlarmMetadata, AlarmStatus, AlarmType}
import csw.services.alarm.client.internal.helpers.AlarmServiceTestSetup
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture

// DEOPSCSW-486: Provide API to load alarm metadata in Alarm store from file
class InitAlarmStoreTest extends AlarmServiceTestSetup {

  test("should load alarms from provided config file") {
    val alarmKey = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")

    val path = getClass.getResource("/test-alarms/valid-alarms.conf").getPath
    val file = new File(path)
    alarmService.initAlarms(file).await

    alarmService.getMetadata(alarmKey).await shouldBe AlarmMetadata(
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

    alarmService.getStatus(alarmKey).await shouldBe AlarmStatus()
    alarmService.getSeverity(alarmKey).await shouldBe Disconnected
    alarmService.getAggregatedHealth(alarmKey).await shouldBe Bad
  }

  test("should reset the previous Alarm data in redis and set newly provided") {

    val threeAlarmConfigPath = getClass.getResource("/test-alarms/valid-alarms.conf").getPath
    val twoAlarmConfigPath   = getClass.getResource("/test-alarms/two-valid-alarms.conf").getPath

    val nfiraosAlarmKey = AlarmKey("nfiraos", "cc.trombone", "tromboneAxisHighLimitAlarm")
    val tcpAlarmKey     = AlarmKey("tcp", "tcsPk", "cpuExceededAlarm")
    val firstFile       = new File(threeAlarmConfigPath)
    val secondFile      = new File(twoAlarmConfigPath)
    alarmService.initAlarms(firstFile).await

    alarmService.initAlarms(secondFile, reset = true).await

    intercept[KeyNotFoundException] {
      alarmService.getMetadata(tcpAlarmKey).await
    }

    alarmService.getMetadata(nfiraosAlarmKey).await shouldBe AlarmMetadata(
      subsystem = "nfiraos",
      component = "cc.trombone",
      name = "tromboneAxisHighLimitAlarm",
      description = "Warns when trombone axis has reached the high limit",
      location = "south side",
      AlarmType.Absolute,
      Set(Indeterminate, Okay, Warning, Major, Critical),
      probableCause = "the trombone software has failed or the stage was driven into the high limit",
      operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command",
      isAutoAcknowledgeable = true,
      isLatchable = true,
      activationStatus = Active
    )
  }

}
