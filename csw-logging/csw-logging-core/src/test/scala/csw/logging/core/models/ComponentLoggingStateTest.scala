package csw.logging.core.models

import csw.logging.core.internal.LoggingLevels.{DEBUG, ERROR}
import org.scalatest.{FunSuite, Matchers}

class ComponentLoggingStateTest extends FunSuite with Matchers {

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
