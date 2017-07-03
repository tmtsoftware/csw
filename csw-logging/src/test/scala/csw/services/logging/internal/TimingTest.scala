package csw.services.logging.internal

import java.nio.file.Paths
import java.time.LocalDateTime

import akka.actor.ActorSystem
import com.persist.JsonOps.JsonObject
import com.typesafe.config.ConfigFactory
import csw.services.logging.appenders.FileAppender
import csw.services.logging.components.TromboneActor
import csw.services.logging.components.TromboneActor._
import csw.services.logging.scaladsl.RequestId
import csw.services.logging.utils.{FileUtils, LoggingTestSuite}

import scala.collection.mutable

class TimingTest extends LoggingTestSuite with Timing {

  private val logFileDir = Paths.get("/tmp/csw-test-logs").toFile
  private val config = ConfigFactory
    .parseString(s"csw-logging.appenders.file.logPath=${logFileDir.getAbsolutePath}")
    .withFallback(ConfigFactory.load)

  private val loggingSystemName = "TimingTest"
  override lazy val actorSystem = ActorSystem("timing-test-system", config)
  override lazy val loggingSystem =
    new LoggingSystem(loggingSystemName, "version", "localhost", actorSystem, Seq(testAppender, FileAppender))

  private val tromboneActorRef =
    actorSystem.actorOf(TromboneActor.props(), name = "TromboneActor")

  private val fileTimestamp   = FileAppender.decideTimestampForFile(LocalDateTime.now())
  private val fullLogFileDir  = logFileDir + "/" + loggingSystemName
  private val timeLogFilePath = fullLogFileDir + s"/time.$fileTimestamp.log"
  private val testLogFilePath = fullLogFileDir + s"/common.$fileTimestamp.log"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    FileUtils.deleteRecursively(logFileDir)
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    FileUtils.deleteRecursively(logFileDir)
  }

  def sendLogMsgToTromboneActor() = {
    tromboneActorRef ! LogTrace
    tromboneActorRef ! LogDebug
    tromboneActorRef ! LogInfo
    tromboneActorRef ! LogWarn
    tromboneActorRef ! LogError
    tromboneActorRef ! LogFatal
  }

  private val timerTestRegionName        = "Timer-Test"
  private val startEndTimeTestRegionName = "Start-End-Time-Test"
  private val timerRegionQueue           = mutable.Queue[String](timerTestRegionName, startEndTimeTestRegionName)

  def logMessagesWithTimer() =
    Time(RequestId(), timerTestRegionName) {
      sendLogMsgToTromboneActor
    }

  def logMessagesWithStartAndEndTimer() = {
    val id         = RequestId()
    val startToken = time.start(id, startEndTimeTestRegionName)
    sendLogMsgToTromboneActor
    time.end(id, startEndTimeTestRegionName, startToken)
  }

  // DEOPSCSW-142: Flexibility of logging approaches
  test("should able to log messages to combination of standard out and file concurrently and also log time messages.") {

    logMessagesWithTimer
    logMessagesWithStartAndEndTimer
    Thread.sleep(3000)

    // Reading time logger file
    val timeLogBuffer = FileUtils.read(timeLogFilePath)
    // Reading common logger file
    val fileLogBuffer = FileUtils.read(testLogFilePath)

    // validating timer logger
    timeLogBuffer.toList.foreach { log ⇒
      val itemsMap = log("items").asInstanceOf[List[JsonObject]].head
      itemsMap("name") shouldBe timerRegionQueue.dequeue
      itemsMap.contains("time0") shouldBe true
      itemsMap.contains("time1") shouldBe true
      itemsMap.contains("total") shouldBe true
      log("@name") shouldBe "TimingTest"
    }

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
