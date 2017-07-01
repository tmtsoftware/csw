package csw.services.logging.appenders

import java.nio.file.Paths
import java.time.LocalDateTime

import akka.actor.ActorSystem
import com.persist.JsonOps.JsonObject
import com.typesafe.config.ConfigFactory
import csw.services.logging.components.TromboneActor
import csw.services.logging.components.TromboneActor._
import csw.services.logging.internal.LoggingSystem
import csw.services.logging.utils.{FileUtils, LoggingTestSuite}

import scala.collection.mutable

class MultiAppenderTest extends LoggingTestSuite {

  private val logFileDir = Paths.get("/tmp/csw-test-logs").toFile
  private val config = ConfigFactory
    .parseString(s"csw-logging.appenders.file.logPath=${logFileDir.getAbsolutePath}")
    .withFallback(ConfigFactory.load)

  private val loggingSystemName = "MultiAppenderTest"
  override lazy val actorSystem = ActorSystem("test-system", config)
  override lazy val loggingSystem =
    new LoggingSystem(loggingSystemName, "version", "localhost", actorSystem, Seq(testAppender, FileAppender))

  private val tromboneActorRef =
    actorSystem.actorOf(TromboneActor.props(), name = "TromboneActor")

  private val fileTimestamp   = FileAppender.decideTimestampForFile(LocalDateTime.now())
  private val fullLogFileDir  = logFileDir + "/" + loggingSystemName
  private val testLogFilePath = fullLogFileDir + s"/common.$fileTimestamp.log"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    FileUtils.deleteRecursively(logFileDir)
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    FileUtils.deleteRecursively(logFileDir)
  }
  def logMessages(): Unit = {

    tromboneActorRef ! LogTrace
    tromboneActorRef ! LogDebug
    tromboneActorRef ! LogInfo
    tromboneActorRef ! LogWarn
    tromboneActorRef ! LogError
    tromboneActorRef ! LogFatal
  }

  // DEOPSCSW-142: Flexibility of logging approaches
  test("should able to log messages to combination of standard out and file concurrently") {
    logMessages()
    Thread.sleep(3000)

    // Reading common logger file
    val fileLogBuffer = FileUtils.read(testLogFilePath)

    // validating file logger
    testLogBuffer(fileLogBuffer)
    // validating console logger
    testLogBuffer(logBuffer)

    def testLogBuffer(logBuffer: mutable.Buffer[JsonObject]): Unit = {
      logBuffer.foreach { log ⇒
        log.contains("@componentName") shouldBe true
        log.contains("actor") shouldBe true
        log("@componentName") shouldBe "tromboneHcdActor"
        log("actor") shouldBe tromboneActorRef.path.toString
        log.contains("file") shouldBe true
        log.contains("line") shouldBe true
        log.contains("class") shouldBe true
      }
    }
  }

}
