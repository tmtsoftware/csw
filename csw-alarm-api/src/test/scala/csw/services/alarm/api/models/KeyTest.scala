package csw.services.alarm.api.models
import csw.params.core.models.Subsystem
import csw.params.core.models.Subsystem.NFIRAOS
import csw.services.alarm.api.models.Key.{AlarmKey, ComponentKey, GlobalKey, SubsystemKey}
import csw.services.alarm.api.internal.Separators.KeySeparator
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-435: Identify Alarm by Subsystem, component and AlarmName
class KeyTest extends FunSuite with Matchers with TableDrivenPropertyChecks {
  test("AlarmKey should be representing a unique alarm") {
    val tromboneAxisHighLimitAlarm = AlarmKey(NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm")
    tromboneAxisHighLimitAlarm.value shouldEqual s"nfiraos${KeySeparator}trombone${KeySeparator}tromboneaxishighlimitalarm"
  }

  test("SubsystemKey should be representing keys for all alarms of a subsystem") {
    val subsystemKey = SubsystemKey(NFIRAOS)
    subsystemKey.value shouldEqual s"nfiraos$KeySeparator*$KeySeparator*"
  }

  test("ComponentKey should be representing keys for all alarms of a component") {
    val subsystemKey = ComponentKey(NFIRAOS, "trombone")
    subsystemKey.value shouldEqual s"nfiraos${KeySeparator}trombone$KeySeparator*"
  }

  test("GlobalKey should be representing keys for all alarms") {
    GlobalKey.value shouldEqual s"*$KeySeparator*$KeySeparator*"
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
