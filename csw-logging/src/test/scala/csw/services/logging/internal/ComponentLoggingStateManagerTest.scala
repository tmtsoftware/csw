package csw.services.logging.internal

import com.typesafe.config.ConfigFactory
import csw.services.logging.internal.LoggingLevels.{DEBUG, ERROR, FATAL, INFO}
import org.scalatest.{FunSuite, Matchers}

class ComponentLoggingStateManagerTest extends FunSuite with Matchers {

  private val config = ConfigFactory.load().getConfig("csw-logging")

  test("should able to parse logging configuration and extract component log levels state") {
    val componentsLoggingState = ComponentLoggingStateManager.from(config)

    componentsLoggingState("tromboneHcd").componentLogLevel shouldBe DEBUG
    componentsLoggingState("tromboneHcdActor").componentLogLevel shouldBe ERROR
    componentsLoggingState("jTromboneHcdActor").componentLogLevel shouldBe INFO
  }

  test("should able to add/update component logging level") {
    val componentsLoggingState = ComponentLoggingStateManager.from(config)

    LoggingState.componentsLoggingState = LoggingState.componentsLoggingState ++ componentsLoggingState

    // adding new component
    ComponentLoggingStateManager.add("tromboneAssembly", INFO)
    LoggingState.componentsLoggingState("tromboneAssembly").componentLogLevel shouldBe INFO

    // updating log level of component
    LoggingState.componentsLoggingState("tromboneHcd").componentLogLevel shouldBe DEBUG
    ComponentLoggingStateManager.add("tromboneHcd", FATAL)
    LoggingState.componentsLoggingState("tromboneHcd").componentLogLevel shouldBe FATAL

    // undoing above change to avoid its affect on other tests
    ComponentLoggingStateManager.add("tromboneHcd", DEBUG)
    LoggingState.componentsLoggingState = LoggingState.componentsLoggingState - "tromboneAssembly"
  }

  test("should return empty map if component log levels are not provided in configuration file") {
    // ConfigFactory.load("csw-logging") brings default app.conf in scope from logging which has empty component-log-levels block
    val componentsLoggingState = ComponentLoggingStateManager.from(ConfigFactory.load("csw-logging"))

    componentsLoggingState.isEmpty shouldBe true
  }
}
