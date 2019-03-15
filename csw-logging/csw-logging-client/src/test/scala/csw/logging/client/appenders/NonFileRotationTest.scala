package csw.logging.client.appenders

import java.nio.file.Paths

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.logging.client.commons.{Category, LoggingKeys}
import csw.logging.client.utils.FileUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}
import play.api.libs.json.{JsObject, Json}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

// DEOPSCSW-281 Rolling File Configuration
// DEOPSCSW-649: Fixed directory configuration for multi JVM scenario
class NonFileRotationTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {
  private val logFileDir = Paths.get("/tmp/csw-test-logs/").toFile
  private val map: Map[String, Any] = Map(
    "csw-logging.appender-config.file.rotate" → false
  )
  private val config                    = ConfigFactory.parseMap(map.asJava).withFallback(ConfigFactory.load())
  private val actorSystem               = ActorSystem("test-1", config)
  private val standardHeaders: JsObject = Json.obj(LoggingKeys.HOST -> "localhost", LoggingKeys.NAME -> "test-service")

  private val fileAppender = new FileAppender(actorSystem, standardHeaders)

  val logMsgString1: String =
    s"""{
      |  "${LoggingKeys.CATEGORY}": "alternative",
      |  "${LoggingKeys.COMPONENT_NAME}": "FileAppenderTest",
      |  "${LoggingKeys.HOST}": "localhost",
      |  "${LoggingKeys.NAME}": "test-service",
      |  "${LoggingKeys.SEVERITY}": "ERROR",
      |  "${LoggingKeys.TIMESTAMP}": "2017-06-19T16:10:19.397Z",
      |  "${LoggingKeys.CLASS}": "csw.logging.client.appenders.FileAppenderTest",
      |  "${LoggingKeys.FILE}": "FileAppenderTest.scala",
      |  "${LoggingKeys.LINE}": 25,
      |  "${LoggingKeys.MESSAGE}": "This is at ERROR level"
      |}
    """.stripMargin

  val logMsgString2: String =
    s"""{
      |  "${LoggingKeys.CATEGORY}": "${Category.Common.name}",
      |  "${LoggingKeys.COMPONENT_NAME}": "FileAppenderTest",
      |  "${LoggingKeys.HOST}": "localhost",
      |  "${LoggingKeys.NAME}": "test-service",
      |  "${LoggingKeys.SEVERITY}": "ERROR",
      |  "${LoggingKeys.TIMESTAMP}": "2017-06-20T16:10:19.397Z",
      |  "${LoggingKeys.CLASS}": "csw.logging.client.appenders.FileAppenderTest",
      |  "${LoggingKeys.FILE}": "FileAppenderTest.scala",
      |  "${LoggingKeys.LINE}": 25,
      |  "${LoggingKeys.MESSAGE}": "This is at ERROR level"
      |}
    """.stripMargin

  val logMsgString3: String =
    s"""{
      |  "${LoggingKeys.CATEGORY}": "${Category.Common.name}",
      |  "${LoggingKeys.COMPONENT_NAME}": "FileAppenderTest",
      |  "${LoggingKeys.HOST}": "localhost",
      |  "${LoggingKeys.NAME}": "test-service",
      |  "${LoggingKeys.SEVERITY}": "INFO",
      |  "${LoggingKeys.TIMESTAMP}": "2017-06-23T01:10:19.397Z",
      |  "${LoggingKeys.CLASS}": "csw.logging.client.appenders.FileAppenderTest",
      |  "${LoggingKeys.FILE}": "FileAppenderTest.scala",
      |  "${LoggingKeys.LINE}": 25,
      |  "${LoggingKeys.MESSAGE}": "This is at INFO level"
      |}
    """.stripMargin

  val expectedLogMsgJson1: JsObject = Json.parse(logMsgString1).as[JsObject]
  val expectedLogMsgJson2: JsObject = Json.parse(logMsgString2).as[JsObject]
  val expectedLogMsgJson3: JsObject = Json.parse(logMsgString3).as[JsObject]

  private val logFileFullPath1 = logFileDir.getAbsolutePath ++ s"/test-service_alternative.log"
  private val logFileFullPath2 = logFileDir.getAbsolutePath ++ s"/test-service.log"

  override protected def beforeAll(): Unit = {
    FileUtils.deleteRecursively(logFileDir)
  }

  override protected def afterAll(): Unit = {
//    FileUtils.deleteRecursively(logFileDir)
    Await.result(actorSystem.terminate(), 5.seconds)
  }

  // DEOPSCSW-281 Rolling File Configuration
  test("verify log file rotation is off") {
    fileAppender.append(expectedLogMsgJson1, "alternative")
    fileAppender.append(expectedLogMsgJson2, Category.Common.name)
    fileAppender.append(expectedLogMsgJson3, Category.Common.name)
    Thread.sleep(10)

    val actualLogBuffer1 = FileUtils.read(logFileFullPath1).toList
    val actualLogBuffer2 = FileUtils.read(logFileFullPath2).toList

    actualLogBuffer1.head shouldBe expectedLogMsgJson1
    actualLogBuffer2.head shouldBe expectedLogMsgJson2
    actualLogBuffer2.tail.head shouldBe expectedLogMsgJson3
  }
}
