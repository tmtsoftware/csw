package example.teskit

import com.typesafe.config.ConfigFactory
import csw.alarm.models.Key.AlarmKey
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.NFIRAOS
import csw.testkit.scaladsl.CSWService.AlarmServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.funsuite.AnyFunSuiteLike

//#scalatest-alarm-testkit
class ScalaAlarmTestKitExampleTest extends ScalaTestFrameworkTestKit(AlarmServer) with AnyFunSuiteLike {
  import frameworkTestKit.alarmTestKit._
  test("test initializing alarms via config") {
    initAlarms(ConfigFactory.parseResources("valid-alarms.conf"))

    // .. assertions etc.
  }

  test("use getCurrentSeverity to fetch severity of initialized alarms") {
    val severity = getCurrentSeverity(AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneAxisLowLimitAlarm"))
    // .. assertions etc.
  }
}
//#scalatest-alarm-testkit