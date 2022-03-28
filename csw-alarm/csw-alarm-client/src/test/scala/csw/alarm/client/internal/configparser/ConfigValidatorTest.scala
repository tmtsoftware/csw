/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client.internal.configparser
import com.typesafe.config.ConfigFactory
import csw.alarm.api.internal.ValidationResult
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

// DEOPSCSW-451: Create set of alarms based on Configuration file
class ConfigValidatorTest extends AnyFunSuite with Matchers {
  test("validation result should be successful for list of valid alarm's metadata | DEOPSCSW-451") {
    val config = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
    ConfigValidator.validate(config, ConfigParser.ALARMS_SCHEMA) shouldEqual ValidationResult.Success
  }

  test("validation result should be failed for invalid alarms metadata | DEOPSCSW-451") {
    val config = ConfigFactory.parseResources("test-alarms/invalid-alarms.conf")
    val result = ConfigValidator.validate(config, ConfigParser.ALARMS_SCHEMA)
    result shouldBe a[ValidationResult.Failure]
  }
}
