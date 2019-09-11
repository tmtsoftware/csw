package csw.logging.client.scaladsl

import java.net.InetAddress

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.logging.client.appenders.{FileAppender, StdOutAppender}
import csw.logging.client.exceptions.AppenderNotFoundException
import csw.logging.client.internal.LoggingSystem
import csw.logging.models.Level
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import play.api.libs.json.JsObject

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class LoggingSystemTest extends FunSuite with Matchers with BeforeAndAfterAll {
  // This will load default configuration in application.conf file if provided
  private var loggingSystem: LoggingSystem = _
  private val config                       = ConfigFactory.load().getConfig("csw-logging")

  override protected def beforeAll(): Unit = loggingSystem = LoggingSystemFactory.start()

  override protected def afterAll(): Unit = {
    loggingSystem.system.terminate()
    Await.result(loggingSystem.system.whenTerminated, 20.seconds)
  }

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

  test("should be able to set log level for default logger, slf4j and akka") {
    val logLevel      = "debug"
    val akkaLogLevel  = "Error"
    val slf4jLogLevel = "INFO"

    loggingSystem.setDefaultLogLevel(Level(akkaLogLevel))
    loggingSystem.setAkkaLevel(Level(logLevel))
    loggingSystem.setSlf4jLevel(Level(slf4jLogLevel))

    loggingSystem.getDefaultLogLevel.current.name.toLowerCase shouldBe akkaLogLevel.toLowerCase
    loggingSystem.getAkkaLevel.current.name.toLowerCase shouldBe logLevel.toLowerCase
    loggingSystem.getSlf4jLevel.current.name.toLowerCase shouldBe slf4jLogLevel.toLowerCase
  }

  // DEOPSCSW-142: Flexibility of logging approaches
  test("should be able to parse appenders from configuration file") {
    loggingSystem.getAppenders.toSet shouldBe Set(StdOutAppender, FileAppender)
  }

  // DEOPSCSW-142: Flexibility of logging approaches
  test("should throw AppenderNotFoundException for an invalid appender configured") {
    val config      = ConfigFactory.parseString("""
        |csw-logging {
        | appenders = ["abcd"]
        |}
      """.stripMargin)
    val actorSystem = ActorSystem(SpawnProtocol.behavior, "test", config)
    val exception = intercept[AppenderNotFoundException] {
      LoggingSystemFactory.start("foo-name", "foo-version", InetAddress.getLocalHost.getHostName, actorSystem)
    }
    exception.appender shouldBe "abcd"
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 10.seconds)
  }
}
