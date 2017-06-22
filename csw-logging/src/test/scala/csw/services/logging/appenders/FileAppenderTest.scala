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
    .parseString(s"com.persist.logging.appenders.file.logPath=${logFileDir.getAbsolutePath}")
    .withFallback(ConfigFactory.load)
  private val actorSystem = ActorSystem("test-1", config)
  private val standardHeaders: Map[String, RichMsg] =
    Map[String, RichMsg]("@host" -> "localhost", "@name" -> "test-service")

  private val fileAppender = new FileAppender(actorSystem, standardHeaders)

  val logMsgString: String =
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

  val expectedLogMsgJson = JsonOps.Json(logMsgString).asInstanceOf[Map[String, String]]

  private val date            = jgetString(expectedLogMsgJson, "timestamp").substring(0, 10)
  private val logFileFullPath = logFileDir.getAbsolutePath ++ s"/test-service/common.$date.log"

  override protected def beforeAll(): Unit = {
    deleteRecursively(logFileDir)
  }

  override protected def afterEach(): Unit = {
//    deleteRecursively(logFileDir)
  }

  override protected def afterAll(): Unit = {
    Await.result(actorSystem.terminate(), 5.seconds)
  }

  test("should log messages to file") {
    fileAppender.append(expectedLogMsgJson, "common")
    Thread.sleep(3000)

    val sourceLogFile = scala.io.Source.fromFile(logFileFullPath)
    val actualLogMsg  = sourceLogFile.mkString
    val actualLogJson = JsonOps.Json(actualLogMsg).asInstanceOf[Map[String, String]]

    actualLogJson shouldBe expectedLogMsgJson

    sourceLogFile.close()
  }

  def deleteRecursively(file: File): Unit = {
    if (file.isDirectory)
      file.listFiles.foreach(deleteRecursively)
    if (file.exists && !file.delete)
      throw new Exception(s"Unable to delete ${file.getAbsolutePath}")
  }

}
