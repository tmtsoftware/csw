package csw.services.logging.appenders

import java.nio.file.Paths

import akka.actor.ActorSystem
import com.persist.JsonOps
import com.persist.JsonOps.jgetString
import com.typesafe.config.ConfigFactory
import csw.services.logging.RichMsg
import csw.services.logging.commons.{Category, Keys, TMTDateTimeFormatter}
import csw.services.logging.utils.FileUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

// DEOPSCSW-123: Allow local component logs to be output to a file
class FileAppenderTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {
  private val logFileDir = Paths.get("/tmp/csw-test-logs/").toFile
  private val config = ConfigFactory
    .parseString(s"csw-logging.appender-config.file.logPath=${logFileDir.getAbsolutePath}")
    .withFallback(ConfigFactory.load)
  private val actorSystem = ActorSystem("test-1", config)
  private val standardHeaders: Map[String, RichMsg] =
    Map[String, RichMsg](Keys.HOST -> "localhost", Keys.NAME -> "test-service")

  private val fileAppender = new FileAppender(actorSystem, standardHeaders)

  val logMsgString1: String =
    s"""{
      |  "${Keys.CATEGORY}": "alternative",
      |  "${Keys.COMPONENT_NAME}": "FileAppenderTest",
      |  "${Keys.HOST}": "localhost",
      |  "${Keys.NAME}": "test-service",
      |  "${Keys.SEVERITY}": "ERROR",
      |  "${Keys.TIMESTAMP}": "2017-06-19T16:10:19:397000000",
      |  "${Keys.CLASS}": "csw.services.logging.appenders.FileAppenderTest",
      |  "${Keys.FILE}": "FileAppenderTest.scala",
      |  "${Keys.LINE}": 25,
      |  "${Keys.MESSAGE}": "This is at ERROR level"
      |}
    """.stripMargin

  val logMsgString2: String =
    s"""{
      |  "${Keys.CATEGORY}": "${Category.Common.name}",
      |  "${Keys.COMPONENT_NAME}": "FileAppenderTest",
      |  "${Keys.HOST}": "localhost",
      |  "${Keys.NAME}": "test-service",
      |  "${Keys.SEVERITY}": "ERROR",
      |  "${Keys.TIMESTAMP}": "2017-06-20T16:10:19:397000000",
      |  "${Keys.CLASS}": "csw.services.logging.appenders.FileAppenderTest",
      |  "${Keys.FILE}": "FileAppenderTest.scala",
      |  "${Keys.LINE}": 25,
      |  "${Keys.MESSAGE}": "This is at ERROR level"
      |}
    """.stripMargin

  val logMsgString3: String =
    s"""{
      |  "${Keys.CATEGORY}": "${Category.Common.name}",
      |  "${Keys.COMPONENT_NAME}": "FileAppenderTest",
      |  "${Keys.HOST}": "localhost",
      |  "${Keys.NAME}": "test-service",
      |  "${Keys.SEVERITY}": "INFO",
      |  "${Keys.TIMESTAMP}": "2017-06-23T01:10:19:397000000",
      |  "${Keys.CLASS}": "csw.services.logging.appenders.FileAppenderTest",
      |  "${Keys.FILE}": "FileAppenderTest.scala",
      |  "${Keys.LINE}": 25,
      |  "${Keys.MESSAGE}": "This is at INFO level"
      |}
    """.stripMargin

  val expectedLogMsgJson1: Map[String, String] = JsonOps.Json(logMsgString1).asInstanceOf[Map[String, String]]
  val expectedLogMsgJson2: Map[String, String] = JsonOps.Json(logMsgString2).asInstanceOf[Map[String, String]]
  val expectedLogMsgJson3: Map[String, String] = JsonOps.Json(logMsgString3).asInstanceOf[Map[String, String]]

  private val date1            = jgetString(expectedLogMsgJson1, Keys.TIMESTAMP)
  private val localDateTime1   = FileAppender.decideTimestampForFile(TMTDateTimeFormatter.parse(date1))
  private val logFileFullPath1 = logFileDir.getAbsolutePath ++ s"/test-service/alternative.$localDateTime1.log"

  private val date2            = jgetString(expectedLogMsgJson2, Keys.TIMESTAMP)
  private val localDateTime2   = FileAppender.decideTimestampForFile(TMTDateTimeFormatter.parse(date2))
  private val logFileFullPath2 = logFileDir.getAbsolutePath ++ s"/test-service/common.$localDateTime2.log"

  private val date3            = jgetString(expectedLogMsgJson3, Keys.TIMESTAMP)
  private val localDateTime3   = FileAppender.decideTimestampForFile(TMTDateTimeFormatter.parse(date3))
  private val logFileFullPath3 = logFileDir.getAbsolutePath ++ s"/test-service/common.$localDateTime3.log"

  override protected def beforeAll(): Unit = {
    FileUtils.deleteRecursively(logFileDir)
  }

  override protected def afterAll(): Unit = {
    FileUtils.deleteRecursively(logFileDir)
    Await.result(actorSystem.terminate(), 5.seconds)
  }

  //DEOPSCSW-151 : Manage log file size
  test("log file is rotated every day") {
    fileAppender.append(expectedLogMsgJson1, "alternative")
    fileAppender.append(expectedLogMsgJson2, Category.Common.name)
    fileAppender.append(expectedLogMsgJson3, Category.Common.name)
    Thread.sleep(3000)

    val actualLogBuffer1 = FileUtils.read(logFileFullPath1).toList
    val actualLogBuffer2 = FileUtils.read(logFileFullPath2).toList
    val actualLogBuffer3 = FileUtils.read(logFileFullPath3).toList

    actualLogBuffer1.head shouldBe expectedLogMsgJson1
    actualLogBuffer2.head shouldBe expectedLogMsgJson2
    actualLogBuffer3.head shouldBe expectedLogMsgJson3
  }
}
