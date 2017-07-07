package csw.services.logging.scaladsl

import java.net.InetAddress

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.services.logging.appenders.{FileAppender, StdOutAppender}
import csw.services.logging.exceptions.AppenderNotFoundException
import csw.services.logging.internal.LoggingLevels.Level
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class LoggingSystemTest extends FunSuite with Matchers with BeforeAndAfterAll {

  // This will load default configuration in application.conf file if provided
  private val loggingSystem = LoggingSystemFactory.start()
  private val config = ConfigFactory.load().getConfig("csw-logging")

  override protected def afterAll(): Unit =
    Await.result(loggingSystem.stop, 10.seconds)
  //  Await.result(system.terminate(), 20 seconds)

  test("should load default log level provided in configuration file") {
    loggingSystem.getDefaultLogLevel.default.name shouldBe config
      .getString("logLevel")
      .toUpperCase
    loggingSystem.getAkkaLevel.default.name shouldBe config
      .getString("akkaLogLevel")
      .toUpperCase
    loggingSystem.getSlf4jLevel.default.name shouldBe config
      .getString("slf4jLogLevel")
      .toUpperCase
  }

  test("should able to set log level for default logger, slf4j and akka") {
    val logLevel = "debug"
    val akkaLogLevel = "Error"
    val slf4jLogLevel = "INFO"

    loggingSystem.setDefaultLogLevel(Level(akkaLogLevel))
    loggingSystem.setAkkaLevel(Level(logLevel))
    loggingSystem.setSlf4jLevel(Level(slf4jLogLevel))

    loggingSystem.getDefaultLogLevel.current.name.toLowerCase shouldBe akkaLogLevel.toLowerCase
    loggingSystem.getAkkaLevel.current.name.toLowerCase shouldBe logLevel.toLowerCase
    loggingSystem.getSlf4jLevel.current.name.toLowerCase shouldBe slf4jLogLevel.toLowerCase
  }

  test("should be able to configure appenders in configuration file") {
    loggingSystem.getAppenders.toSet shouldBe Set(StdOutAppender, FileAppender)
  }

  test(
    "should throw AppenderNotFoundException for an invalid appender configured") {
    val config = ConfigFactory.parseString("""
        |csw-logging {
        | appenders = ["abcd"]
        |}
      """.stripMargin)
    val actorSystem = ActorSystem("test", config)
    val exception = intercept[AppenderNotFoundException] {
      LoggingSystemFactory.start("foo-name",
                                 "foo-version",
                                 InetAddress.getLocalHost.getHostName,
                                 actorSystem)
    }
    exception.appender shouldBe "abcd"
    Await.result(actorSystem.terminate(), 10.seconds)
  }
}
