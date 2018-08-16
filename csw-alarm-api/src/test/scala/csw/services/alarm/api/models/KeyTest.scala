package csw.services.alarm.api.models
import csw.messages.params.models.Subsystem
import csw.messages.params.models.Subsystem.NFIRAOS
import csw.services.alarm.api.models.Key.{AlarmKey, ComponentKey, GlobalKey, SubsystemKey}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-435: Identify Alarm by Subsystem, component and AlarmName
class KeyTest extends FunSuite with Matchers with TableDrivenPropertyChecks {
  test("AlarmKey should be representing a unique alarm") {
    val tromboneAxisHighLimitAlarm = AlarmKey(NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm")
    tromboneAxisHighLimitAlarm.value shouldEqual "nfiraos.trombone.tromboneaxishighlimitalarm"
  }

  test("SubsystemKey should be representing keys for all alarms of a subsystem") {
    val subsystemKey = SubsystemKey(NFIRAOS)
    subsystemKey.value shouldEqual "nfiraos.*.*"
  }

  test("ComponentKey should be representing keys for all alarms of a component") {
    val subsystemKey = ComponentKey(NFIRAOS, "trombone")
    subsystemKey.value shouldEqual "nfiraos.trombone.*"
  }

  test("GlobalKey should be representing keys for all alarms") {
    GlobalKey.value shouldEqual "*.*.*"
  }

  val invalidCharacers = List("*", "[", "]", "-", "^")

  invalidCharacers.foreach(character => {
    test(s"AlarmKey should not allow '$character' character") {
      intercept[IllegalArgumentException] {
        AlarmKey(NFIRAOS, "trombone", character)
      }
    }
  })

  invalidCharacers.foreach(character => {
    test(s"ComponentKey should not allow '$character' character") {
      intercept[IllegalArgumentException] {
        ComponentKey(NFIRAOS, character)
      }
    }
  })

  test("ComponentKey should not allow empty values") {
    intercept[IllegalArgumentException] {
      ComponentKey(Subsystem.TEST, null)
    }
  }

  test("AlarmKey should not allow empty values") {
    intercept[IllegalArgumentException] {
      Key.AlarmKey(Subsystem.TEST, "test", "")
    }
  }
}
