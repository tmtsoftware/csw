package csw.services.alarm.api.models
import csw.services.alarm.api.models.Key.{AlarmKey, ComponentKey, GlobalKey, SubsystemKey}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-435: Identify Alarm by Subsystem, component and AlarmName
class KeyTest extends FunSuite with Matchers with TableDrivenPropertyChecks {
  test("AlarmKey should be representing a unique alarm") {
    val tromboneAxisHighLimitAlarm = AlarmKey("nfiraos", "trombone", "tromboneAxisHighLimitAlarm")
    tromboneAxisHighLimitAlarm.value shouldEqual "nfiraos.trombone.tromboneaxishighlimitalarm"
  }

  test("SubsystemKey should be representing keys for all alarms of a subsystem") {
    val subsystemKey = SubsystemKey("nfiraos")
    subsystemKey.value shouldEqual "nfiraos.*.*"
  }

  test("ComponentKey should be representing keys for all alarms of a component") {
    val subsystemKey = ComponentKey("nfiraos", "trombone")
    subsystemKey.value shouldEqual "nfiraos.trombone.*"
  }

  test("GlobalKey should be representing keys for all alarms") {
    GlobalKey.value shouldEqual "*.*.*"
  }

  val invalidCharacers = List("*", "[", "]", "-", "^")

  invalidCharacers.foreach(character => {
    test(s"AlarmKey should not allow '$character' character") {
      intercept[IllegalArgumentException] {
        AlarmKey("nfiraos", "trombone", character)
      }
    }
  })

  invalidCharacers.foreach(character => {
    test(s"ComponentKey should not allow '$character' character") {
      intercept[IllegalArgumentException] {
        ComponentKey("nfiraos", character)
      }
    }
  })

  invalidCharacers.foreach(character => {
    test(s"SubsystemKey should not allow '$character' character") {
      intercept[IllegalArgumentException] {
        SubsystemKey(character)
      }
    }
  })

  test("SubsystemKey should not allow empty subsystem") {
    intercept[IllegalArgumentException] {
      SubsystemKey("")
    }
  }

  test("ComponentKey should not allow empty values") {
    intercept[IllegalArgumentException] {
      ComponentKey("test", null)
    }
  }

  test("AlarmKey should not allow empty values") {
    intercept[IllegalArgumentException] {
      Key.AlarmKey("test", "test", "")
    }
  }
}
