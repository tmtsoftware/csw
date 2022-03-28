/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.models

import csw.logging.models.Level.{DEBUG, ERROR}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ComponentLoggingStateTest extends AnyFunSuite with Matchers {

  test("should initialize component logging state with provided level") {
    val componentLoggingState = ComponentLoggingState(DEBUG)

    componentLoggingState.componentLogLevel shouldBe DEBUG
    componentLoggingState.doTrace shouldBe false
    componentLoggingState.doDebug shouldBe true
    componentLoggingState.doInfo shouldBe true
    componentLoggingState.doWarn shouldBe true
    componentLoggingState.doError shouldBe true
    componentLoggingState.doFatal shouldBe true
  }

  test("should able to update the log level of component") {
    // initializing component logging state with DEBUG level
    val componentLoggingState = ComponentLoggingState(DEBUG)

    // updating component log level to ERROR
    componentLoggingState.setLevel(ERROR)

    componentLoggingState.componentLogLevel shouldBe ERROR
    componentLoggingState.doTrace shouldBe false
    componentLoggingState.doDebug shouldBe false
    componentLoggingState.doInfo shouldBe false
    componentLoggingState.doWarn shouldBe false
    componentLoggingState.doError shouldBe true
    componentLoggingState.doFatal shouldBe true

  }
}
