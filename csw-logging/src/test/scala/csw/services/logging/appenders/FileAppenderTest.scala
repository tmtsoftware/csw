package csw.services.logging.appenders

import java.nio.file.Paths
import java.time.LocalDateTime

import akka.actor.ActorSystem
import com.persist.JsonOps
import com.persist.JsonOps.jgetString
import com.typesafe.config.ConfigFactory
import csw.services.logging.RichMsg
import csw.services.logging.commons.Constants
import csw.services.logging.utils.FileUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

// DEOPSCSW-123: Allow local component logs to be output to a file
class FileAppenderTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {
  private val logFileDir = Paths.get("/tmp/csw-test-logs/").toFile
  private val config = ConfigFactory
    .parseString(s"csw-logging.appenders.file.logPath=${logFileDir.getAbsolutePath}")
    .withFallback(ConfigFactory.load)
  private val actorSystem = ActorSystem("test-1", config)
  private val standardHeaders: Map[String, RichMsg] =
    Map[String, RichMsg]("@host" -> "localhost", "@name" -> "test-service")

  private val fileAppender = new FileAppender(actorSystem, standardHeaders)

  val logMsgString1: String =
    """{
      |  "@category": "alternative",
      |  "@componentName": "FileAppenderTest",
      |  "@host": "localhost",
      |  "@name": "test-service",
      |  "@severity": "ERROR",
      |  "timestamp": "2017-06-19T16:10:19.397000000+05:30",
      |  "class": "csw.services.logging.appenders.FileAppenderTest",
      |  "file": "FileAppenderTest.scala",
      |  "line": 25,
      |  "message": "This is at ERROR level"
      |}
    """.stripMargin

  val logMsgString2: String =
    """{
      |  "@category": "common",
      |  "@componentName": "FileAppenderTest",
      |  "@host": "localhost",
      |  "@name": "test-service",
      |  "@severity": "ERROR",
      |  "timestamp": "2017-06-20T16:10:19.397000000+05:30",
      |  "class": "csw.services.logging.appenders.FileAppenderTest",
      |  "file": "FileAppenderTest.scala",
      |  "line": 25,
      |  "message": "This is at ERROR level"
      |}
    """.stripMargin

  val logMsgString3: String =
    """{
      |  "@category": "common",
      |  "@componentName": "FileAppenderTest",
      |  "@host": "localhost",
      |  "@name": "test-service",
      |  "@severity": "INFO",
      |  "timestamp": "2017-06-23T01:10:19.397000000+05:30",
      |  "class": "csw.services.logging.appenders.FileAppenderTest",
      |  "file": "FileAppenderTest.scala",
      |  "line": 25,
      |  "message": "This is at INFO level"
      |}
    """.stripMargin

  val expectedLogMsgJson1: Map[String, String] = JsonOps.Json(logMsgString1).asInstanceOf[Map[String, String]]
  val expectedLogMsgJson2: Map[String, String] = JsonOps.Json(logMsgString2).asInstanceOf[Map[String, String]]
  val expectedLogMsgJson3: Map[String, String] = JsonOps.Json(logMsgString3).asInstanceOf[Map[String, String]]

  private val date1            = jgetString(expectedLogMsgJson1, "timestamp")
  private val localDateTime1   = FileAppender.decideTimestampForFile(LocalDateTime.parse(date1, Constants.ISOLogFmt))
  private val logFileFullPath1 = logFileDir.getAbsolutePath ++ s"/test-service/alternative.$localDateTime1.log"

  private val date2            = jgetString(expectedLogMsgJson2, "timestamp")
  private val localDateTime2   = FileAppender.decideTimestampForFile(LocalDateTime.parse(date2, Constants.ISOLogFmt))
  private val logFileFullPath2 = logFileDir.getAbsolutePath ++ s"/test-service/common.$localDateTime2.log"

  private val date3            = jgetString(expectedLogMsgJson3, "timestamp")
  private val localDateTime3   = FileAppender.decideTimestampForFile(LocalDateTime.parse(date3, Constants.ISOLogFmt))
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
    fileAppender.append(expectedLogMsgJson2, "common")
    fileAppender.append(expectedLogMsgJson3, "common")
    Thread.sleep(3000)

    val actualLogBuffer1 = FileUtils.read(logFileFullPath1).toList
    val actualLogBuffer2 = FileUtils.read(logFileFullPath2).toList
    val actualLogBuffer3 = FileUtils.read(logFileFullPath3).toList

    actualLogBuffer1.head shouldBe expectedLogMsgJson1
    actualLogBuffer2.head shouldBe expectedLogMsgJson2
    actualLogBuffer3.head shouldBe expectedLogMsgJson3
  }
}
