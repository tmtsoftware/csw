/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.auth

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CommandKeyTest extends AnyFunSuite with Matchers {

  test("should successfully parse key string and store it in lowercase | ESW-95") {
    CommandKey("IRIS.filter.wheel.move").key shouldBe "iris.filter.wheel.move"
  }

  test("should fail to parse key string when subsystem is absent | ESW-95") {
    a[RuntimeException] shouldBe thrownBy(CommandKey("filter.wheel.move"))
  }

}
