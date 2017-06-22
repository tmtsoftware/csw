package csw.services.logging.appenders

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import com.persist.JsonOps
import com.persist.JsonOps.jgetString
import com.typesafe.config.ConfigFactory
import csw.services.logging.RichMsg
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

  val logMsgStringDay1: String =
    """{
      |  "@category": "common",
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

  val logMsgStringDay2: String =
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

  val expectedLogMsgJsonDay1: Map[String, String] = JsonOps.Json(logMsgStringDay1).asInstanceOf[Map[String, String]]
  val expectedLogMsgJsonDay2: Map[String, String] = JsonOps.Json(logMsgStringDay2).asInstanceOf[Map[String, String]]

  private val date1               = jgetString(expectedLogMsgJsonDay1, "timestamp").substring(0, 10)
  private val logFileFullPathDay1 = logFileDir.getAbsolutePath ++ s"/test-service/common.$date1.log"

  private val date2               = jgetString(expectedLogMsgJsonDay2, "timestamp").substring(0, 10)
  private val logFileFullPathDay2 = logFileDir.getAbsolutePath ++ s"/test-service/common.$date2.log"

  override protected def beforeAll(): Unit = {
    deleteRecursively(logFileDir)
  }

  override protected def afterAll(): Unit = {
    deleteRecursively(logFileDir)
    Await.result(actorSystem.terminate(), 5.seconds)
  }

  //DEOPSCSW-151 : Manage log file size
  test("should log messages to file") {
    fileAppender.append(expectedLogMsgJsonDay1, "common")
    fileAppender.append(expectedLogMsgJsonDay2, "common")
    Thread.sleep(3000)

    val sourceLogFileDay1 = scala.io.Source.fromFile(logFileFullPathDay1)
    val sourceLogFileDay2 = scala.io.Source.fromFile(logFileFullPathDay2)

    val actualLogMsgDay1 = sourceLogFileDay1.mkString
    val actualLogMsgDay2 = sourceLogFileDay2.mkString

    val actualLogJsonDay1 = JsonOps.Json(actualLogMsgDay1).asInstanceOf[Map[String, String]]
    val actualLogJsonDay2 = JsonOps.Json(actualLogMsgDay2).asInstanceOf[Map[String, String]]

    actualLogJsonDay1 shouldBe expectedLogMsgJsonDay1
    actualLogJsonDay2 shouldBe expectedLogMsgJsonDay2

    sourceLogFileDay1.close()
    sourceLogFileDay2.close()
  }

  def deleteRecursively(file: File): Unit = {
    if (file.isDirectory)
      file.listFiles.foreach(deleteRecursively)
    if (file.exists && !file.delete)
      throw new Exception(s"Unable to delete ${file.getAbsolutePath}")
  }

}
