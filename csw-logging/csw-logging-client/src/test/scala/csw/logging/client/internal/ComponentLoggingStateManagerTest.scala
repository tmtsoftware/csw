package csw.logging.client.internal

import com.typesafe.config.ConfigFactory
import csw.logging.api.models.LoggingLevels.{DEBUG, ERROR, FATAL, INFO}
import org.scalatest.{FunSuite, Matchers}

class ComponentLoggingStateManagerTest extends FunSuite with Matchers {

  private val config = ConfigFactory.load().getConfig("csw-logging")

  test("should able to parse logging configuration and extract component log levels state") {
    val componentsLoggingState = ComponentLoggingStateManager.from(config)

    componentsLoggingState.get("tromboneHcd").componentLogLevel shouldBe DEBUG
    componentsLoggingState.get("IRIS").componentLogLevel shouldBe ERROR
    componentsLoggingState.get("jTromboneHcdActor").componentLogLevel shouldBe INFO
  }

  test("should able to add/update component logging level") {
    val componentsLoggingState = ComponentLoggingStateManager.from(config)

    LoggingState.componentsLoggingState.putAll(componentsLoggingState)

    // adding new component
    ComponentLoggingStateManager.add("tromboneAssembly", INFO)
    LoggingState.componentsLoggingState.get("tromboneAssembly").componentLogLevel shouldBe INFO

    // updating log level of component
    LoggingState.componentsLoggingState.get("tromboneHcd").componentLogLevel shouldBe DEBUG
    ComponentLoggingStateManager.add("tromboneHcd", FATAL)
    LoggingState.componentsLoggingState.get("tromboneHcd").componentLogLevel shouldBe FATAL

    // undoing above change to avoid its affect on other tests
    ComponentLoggingStateManager.add("tromboneHcd", DEBUG)
    LoggingState.componentsLoggingState.remove("tromboneAssembly")
  }

  test("should return empty map if component log levels are not provided in configuration file") {
    // ConfigFactory.load("csw-logging") brings default app.conf in scope from logging which has empty component-log-levels block
    val componentsLoggingState = ComponentLoggingStateManager.from(ConfigFactory.load("csw-logging"))

    componentsLoggingState.isEmpty shouldBe true
  }
}
