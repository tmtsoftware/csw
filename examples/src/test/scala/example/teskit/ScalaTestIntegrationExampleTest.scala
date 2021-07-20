package example.teskit

//#scalatest-testkit
import com.typesafe.config.ConfigFactory
import csw.alarm.models.Key.AlarmKey
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.NFIRAOS
import csw.testkit.scaladsl.CSWService.{AlarmServer, EventServer}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.funsuite.AnyFunSuiteLike

class ScalaTestIntegrationExampleTest extends ScalaTestFrameworkTestKit(AlarmServer, EventServer) with AnyFunSuiteLike {

  test("test spawning component in standalone mode") {
    spawnStandalone(ConfigFactory.load("SampleHcdStandalone.conf"))

    // .. assertions etc.

  }

}
//#scalatest-testkit
//#scalatest-alarm-testkit
class ScalaAlarmTestKitExampleTest extends ScalaTestFrameworkTestKit(AlarmServer) with AnyFunSuiteLike {
  import frameworkTestKit.alarmTestKit._
  test("test initializing alarms via config") {
    initAlarms(ConfigFactory.load("valid-alarms.conf"))

    // .. assertions etc.
  }

  test("use getCurrentSeverity to fetch severity of initialized alarms") {
    val severity = getCurrentSeverity(AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneAxisLowLimitAlarm"))
    // .. assertions etc.
  }
}
//#scalatest-alarm-testkit
