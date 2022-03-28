/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.models
import csw.alarm.commons.Separators.KeySeparator
import csw.alarm.models.Key.{AlarmKey, ComponentKey, GlobalKey, SubsystemKey}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.NFIRAOS
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

// DEOPSCSW-435: Identify Alarm by Subsystem, component and AlarmName
// CSW-83: Alarm models should take prefix
class KeyTest extends AnyFunSuite with Matchers with TableDrivenPropertyChecks {
  test("AlarmKey should be representing a unique alarm | DEOPSCSW-435") {
    val tromboneAxisHighLimitAlarm = AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneAxisHighLimitAlarm")
    tromboneAxisHighLimitAlarm.value shouldEqual s"nfiraos${KeySeparator}trombone${KeySeparator}tromboneaxishighlimitalarm"
  }

  test("SubsystemKey should be representing keys for all alarms of a subsystem | DEOPSCSW-435") {
    val subsystemKey = SubsystemKey(NFIRAOS)
    subsystemKey.value shouldEqual s"nfiraos$KeySeparator*$KeySeparator*"
  }

  test("ComponentKey should be representing keys for all alarms of a component | DEOPSCSW-435") {
    val subsystemKey = ComponentKey(Prefix(NFIRAOS, "trombone"))
    subsystemKey.value shouldEqual s"nfiraos${KeySeparator}trombone$KeySeparator*"
  }

  test("GlobalKey should be representing keys for all alarms | DEOPSCSW-435") {
    GlobalKey.value shouldEqual s"*$KeySeparator*$KeySeparator*"
  }

  val invalidCharacers = List("*", "[", "]", "-", "^")

  invalidCharacers.foreach(character => {
    test(s"AlarmKey should not allow '$character' character") {
      intercept[IllegalArgumentException] {
        AlarmKey(Prefix(NFIRAOS, "trombone"), character)
      }
    }
  })

  invalidCharacers.foreach(character => {
    test(s"ComponentKey should not allow '$character' character") {
      intercept[IllegalArgumentException] {
        ComponentKey(Prefix(NFIRAOS, character))
      }
    }
  })

  test("ComponentKey should not allow empty values | DEOPSCSW-435") {
    intercept[IllegalArgumentException] {
      ComponentKey(Prefix(NFIRAOS, ""))
    }
  }

  test("AlarmKey should not allow empty values | DEOPSCSW-435") {
    intercept[IllegalArgumentException] {
      Key.AlarmKey(Prefix(NFIRAOS, "test"), "")
    }
  }
}
