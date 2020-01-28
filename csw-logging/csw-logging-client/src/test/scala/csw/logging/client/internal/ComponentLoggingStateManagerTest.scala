package csw.logging.client.internal

import com.typesafe.config.ConfigFactory
import csw.logging.models.Level.{DEBUG, ERROR, FATAL, INFO}
import csw.prefix.models.Prefix
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ComponentLoggingStateManagerTest extends AnyFunSuite with Matchers {

  private val config = ConfigFactory.load().getConfig("csw-logging")

  test("should able to parse logging configuration and extract component log levels state") {
    val componentsLoggingState = ComponentLoggingStateManager.from(config)

    componentsLoggingState.get(Prefix("csw.tromboneHcd")).componentLogLevel shouldBe DEBUG
    componentsLoggingState.get(Prefix("csw.IRIS")).componentLogLevel shouldBe ERROR
    componentsLoggingState.get(Prefix("csw.jTromboneHcdActor")).componentLogLevel shouldBe INFO
  }

  test("should able to add/update component logging level") {
    val componentsLoggingState = ComponentLoggingStateManager.from(config)

    LoggingState.componentsLoggingState.putAll(componentsLoggingState)

    // adding new component
    ComponentLoggingStateManager.add(Prefix("csw.tromboneAssembly"), INFO)
    LoggingState.componentsLoggingState.get(Prefix("csw.tromboneAssembly")).componentLogLevel shouldBe INFO

    // updating log level of component
    LoggingState.componentsLoggingState.get(Prefix("csw.tromboneHcd")).componentLogLevel shouldBe DEBUG
    ComponentLoggingStateManager.add(Prefix("csw.tromboneHcd"), FATAL)
    LoggingState.componentsLoggingState.get(Prefix("csw.tromboneHcd")).componentLogLevel shouldBe FATAL

    // undoing above change to avoid its affect on other tests
    ComponentLoggingStateManager.add(Prefix("csw.tromboneHcd"), DEBUG)
    LoggingState.componentsLoggingState.remove(Prefix("csw.tromboneAssembly"))
  }

  test("should return empty map if component log levels are not provided in configuration file") {
    // ConfigFactory.load("csw-logging") brings default app.conf in scope from logging which has empty component-log-levels block
    val componentsLoggingState = ComponentLoggingStateManager.from(ConfigFactory.load("csw-logging"))

    componentsLoggingState.isEmpty shouldBe true
  }
}
