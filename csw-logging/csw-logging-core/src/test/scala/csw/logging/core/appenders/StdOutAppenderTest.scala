package csw.logging.core.appenders

import java.io.ByteArrayOutputStream

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.logging.core.commons.{Category, LoggingKeys}
import csw.logging.core.internal.JsonExtensions.RichJsObject
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

// DEOPSCSW-122: Allow local component logs to be output to STDOUT
class StdOutAppenderTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  private val actorSystem = ActorSystem("test-1")

  private val standardHeaders: JsObject = Json.obj(
    LoggingKeys.VERSION -> 1,
    LoggingKeys.EX      -> "localhost",
    LoggingKeys.SERVICE -> Json.obj("name" -> "test-service", "version" -> "1.2.3")
  )

  private val stdOutAppender = new StdOutAppender(actorSystem, standardHeaders, println)

  private val logMessage: String =
    s"""{
      |  "${LoggingKeys.COMPONENT_NAME}": "FileAppenderTest",
      |  "${LoggingKeys.HOST}": "localhost",
      |  "${LoggingKeys.SERVICE}": {
      |    "name": "logging",
      |    "version": "SNAPSHOT-1.0"
      |  },
      |  "${LoggingKeys.SEVERITY}": "ERROR",
      |  "${LoggingKeys.TIMESTAMP}": "2017-06-19T16:10:19.397Z",
      |  "${LoggingKeys.VERSION}": 1,
      |  "${LoggingKeys.CLASS}": "csw.logging.core.appenders.FileAppenderTest",
      |  "${LoggingKeys.FILE}": "FileAppenderTest.scala",
      |  "${LoggingKeys.LINE}": 25,
      |  "${LoggingKeys.MESSAGE}": "This is at ERROR level",
      |  "${LoggingKeys.PLAINSTACK}": "exceptions.AppenderNotFoundException at csw.logging.Main (Main.scala 19)"
      |}
    """.stripMargin

  private val expectedLogJson = Json.parse(logMessage).as[JsObject]

  private val outCapture = new ByteArrayOutputStream

  override protected def afterEach(): Unit = {
    outCapture.reset()
  }

  override protected def afterAll(): Unit = {
    outCapture.close()
    Await.result(actorSystem.terminate(), 5.seconds)
  }

  test("should print message to standard output stream if category is \'common\'") {

    Console.withOut(outCapture) {
      stdOutAppender.append(expectedLogJson, Category.Common.name)
    }

    val actualLogJson = Json.parse(outCapture.toString).as[JsObject]
    actualLogJson shouldBe expectedLogJson
  }

  test("should not print message to standard output stream if category is not \'common\'") {
    val category = "foo"

    Console.withOut(outCapture) {
      stdOutAppender.append(expectedLogJson, category)
    }

    outCapture.toString.isEmpty shouldBe true
  }

  // DEOPSCSW-325: Include exception stack trace in stdout log message for exceptions
  test("should able to pretty-print one log message to one line") {

    val config = ConfigFactory
      .parseString("csw-logging.appender-config.stdout.oneLine=true")
      .withFallback(ConfigFactory.load())

    val actorSystemWithOneLineTrueConfig = ActorSystem("test-2", config)
    val stdOutAppenderForOneLineMsg      = new StdOutAppender(actorSystemWithOneLineTrueConfig, standardHeaders, println)

    Console.withOut(outCapture) {
      stdOutAppenderForOneLineMsg.append(expectedLogJson, Category.Common.name)
    }

    val actualOneLineLogMsg   = outCapture.toString.replace("\n", "")
    val severity              = expectedLogJson.getString(LoggingKeys.SEVERITY)
    val msg                   = expectedLogJson.getString(LoggingKeys.MESSAGE)
    val fileName              = expectedLogJson.getString(LoggingKeys.FILE)
    val lineNumber            = expectedLogJson.getString(LoggingKeys.LINE)
    val plainStack            = expectedLogJson.getString(LoggingKeys.PLAINSTACK)
    val timestamp             = expectedLogJson.getString(LoggingKeys.TIMESTAMP)
    val component             = expectedLogJson.getString(LoggingKeys.COMPONENT_NAME)
    val expectedOneLineLogMsg = f"$timestamp $severity%-5s $component ($fileName $lineNumber) - $msg [Stacktrace] $plainStack"

    actualOneLineLogMsg shouldBe expectedOneLineLogMsg

    Await.result(actorSystemWithOneLineTrueConfig.terminate(), 5.seconds)
  }

}
