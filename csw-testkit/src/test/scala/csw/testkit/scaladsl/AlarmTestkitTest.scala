package csw.testkit.scaladsl

import com.typesafe.config.ConfigFactory
import csw.alarm.models.FullAlarmSeverity.Disconnected
import csw.alarm.models.Key.AlarmKey
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.{AlarmServer, LocationServer}
import org.scalatest.funsuite.AnyFunSuiteLike

class AlarmTestkitTest extends ScalaTestFrameworkTestKit(LocationServer, AlarmServer)
  with AnyFunSuiteLike {
  private val alarmTestKit = frameworkTestKit.alarmTestKit

  test("should get severity for initialized alarms with alarm testkit | CSW-21") {
    val config = ConfigFactory.parseResources("valid-alarms.conf")
    alarmTestKit.initAlarms(config, reset = true)
    val alarmKey = AlarmKey(Prefix(Subsystem.NFIRAOS, "trombone"), "tromboneAxisLowLimitAlarm")

    val severity = alarmTestKit.getCurrentSeverity(alarmKey)
    severity shouldBe Disconnected
  }
}
