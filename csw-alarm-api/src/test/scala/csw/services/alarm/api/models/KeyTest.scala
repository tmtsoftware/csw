package csw.services.alarm.api.models
import csw.services.alarm.api.models.Key.{AlarmKey, ComponentKey, GlobalKey, SubsystemKey}
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-435: Identify Alarm by Subsystem, component and AlarmName
class KeyTest extends FunSuite with Matchers {
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

  test("AlarmKey should not allow character '*'") {
    intercept[IllegalArgumentException] {
      AlarmKey("nfiraos", "trombone", "*")
    }
  }

  test("AlarmKey should not allow character '['") {
    intercept[IllegalArgumentException] {
      AlarmKey("nfiraos", "[", "tromboneAxisHighLimitAlarm")
    }
  }

  test("AlarmKey should not allow character ']'") {
    intercept[IllegalArgumentException] {
      AlarmKey("]", "trombone", "tromboneAxisHighLimitAlarm")
    }
  }

  test("AlarmKey should not allow character '-'") {
    intercept[IllegalArgumentException] {
      AlarmKey("nfiraos", "trombone", "-")
    }
  }

  test("AlarmKey should not allow character '^'") {
    intercept[IllegalArgumentException] {
      AlarmKey("nfiraos", "trombone", "^")
    }
  }
}
