package csw.services.logging.appenders

import java.io.ByteArrayOutputStream

import akka.actor.ActorSystem
import com.persist.JsonOps
import com.typesafe.config.ConfigFactory
import csw.services.logging.RichMsg
import csw.services.logging.commons.{Category, LoggingKeys}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

// DEOPSCSW-122: Allow local component logs to be output to STDOUT
class StdOutAppenderTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  private val actorSystem = ActorSystem("test-1")

  private val standardHeaders: Map[String, RichMsg] =
    Map[String, RichMsg](LoggingKeys.VERSION -> 1, LoggingKeys.EX -> "localhost",
      LoggingKeys.SERVICE                    -> Map[String, RichMsg]("name" -> "test-service", "version" -> "1.2.3"))

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
      |  "${LoggingKeys.TIMESTAMP}": "2017-06-19T16:10:19.397000000+05:30",
      |  "${LoggingKeys.VERSION}": 1,
      |  "${LoggingKeys.CLASS}": "csw.services.logging.appenders.FileAppenderTest",
      |  "${LoggingKeys.FILE}": "FileAppenderTest.scala",
      |  "${LoggingKeys.LINE}": 25,
      |  "${LoggingKeys.MESSAGE}": "This is at ERROR level"
      |}
    """.stripMargin

  private val expectedLogJson = JsonOps.Json(logMessage).asInstanceOf[Map[String, String]]

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

    val actualLogJson = JsonOps.Json(outCapture.toString).asInstanceOf[Map[String, String]]
    actualLogJson shouldBe expectedLogJson
  }

  test("should not print message to standard output stream if category is not \'common\'") {
    val category = "foo"

    Console.withOut(outCapture) {
      stdOutAppender.append(expectedLogJson, category)
    }

    outCapture.toString.isEmpty shouldBe true
  }

  test("should able to pretty-print one log message to one line") {

    val config = ConfigFactory
      .parseString("csw-logging.appender-config.stdout.oneLine=true")
      .withFallback(ConfigFactory.load)

    val actorSystemWithOneLineTrueConfig = ActorSystem("test-2", config)
    val stdOutAppenderForOneLineMsg      = new StdOutAppender(actorSystemWithOneLineTrueConfig, standardHeaders, println)

    Console.withOut(outCapture) {
      stdOutAppenderForOneLineMsg.append(expectedLogJson, Category.Common.name)
    }

    val actualOneLineLogMsg   = outCapture.toString.replace("\n", "")
    val severity              = expectedLogJson(LoggingKeys.SEVERITY)
    val msg                   = expectedLogJson(LoggingKeys.MESSAGE)
    val fileName              = expectedLogJson(LoggingKeys.FILE)
    val lineNumber            = s"${expectedLogJson(LoggingKeys.LINE)}"
    val expectedOneLineLogMsg = s"[$severity] $msg ($fileName $lineNumber)"

    actualOneLineLogMsg shouldBe expectedOneLineLogMsg

    Await.result(actorSystemWithOneLineTrueConfig.terminate(), 5.seconds)
  }

}
