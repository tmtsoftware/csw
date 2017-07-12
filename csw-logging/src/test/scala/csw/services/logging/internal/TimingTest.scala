package csw.services.logging.internal

import java.nio.file.Paths
import java.time.{ZoneId, ZoneOffset, ZonedDateTime}

import akka.actor.ActorSystem
import com.persist.JsonOps.JsonObject
import com.typesafe.config.ConfigFactory
import csw.services.logging.appenders.FileAppender
import csw.services.logging.commons.LoggingKeys
import csw.services.logging.components.IrisSupervisorActor
import csw.services.logging.components.IrisSupervisorActor._
import csw.services.logging.scaladsl.RequestId
import csw.services.logging.utils.{FileUtils, LoggingTestSuite}

import scala.collection.mutable

class TimingTest extends LoggingTestSuite with Timing {

  private val logFileDir = Paths.get("/tmp/csw-test-logs").toFile
  private val config = ConfigFactory
    .parseString(s"csw-logging.appender-config.file.logPath=${logFileDir.getAbsolutePath}")
    .withFallback(ConfigFactory.load)

  private val loggingSystemName = "TimingTest"
  override lazy val actorSystem = ActorSystem("timing-test-system", config)
  override lazy val loggingSystem =
    new LoggingSystem(loggingSystemName, "version", "localhost", actorSystem)

  private val irisActorRef =
    actorSystem.actorOf(IrisSupervisorActor.props(), name = "IRIS-Supervisor-Actor")

  private val fileTimestamp   = FileAppender.decideTimestampForFile(ZonedDateTime.now(ZoneId.from(ZoneOffset.UTC)))
  private val fullLogFileDir  = logFileDir + "/" + loggingSystemName
  private val timeLogFilePath = fullLogFileDir + s"/time.$fileTimestamp.log"
  private val testLogFilePath = fullLogFileDir + s"/common.$fileTimestamp.log"
  private val IRIS_NAME       = "IRIS"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    FileUtils.deleteRecursively(logFileDir)
    loggingSystem.setAppenders(List(testAppender, FileAppender))
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    FileUtils.deleteRecursively(logFileDir)
  }

  def sendLogMsgToTromboneActor(): Unit = {
    irisActorRef ! LogTrace
    irisActorRef ! LogDebug
    irisActorRef ! LogInfo
    irisActorRef ! LogWarn
    irisActorRef ! LogError
    irisActorRef ! LogFatal
  }

  private val timerTestRegionName        = "Timer-Test"
  private val startEndTimeTestRegionName = "Start-End-Time-Test"
  private val timerRegionQueue           = mutable.Queue[String](timerTestRegionName, startEndTimeTestRegionName)

  def logMessagesWithTimer(): Unit =
    Time(RequestId(), timerTestRegionName) {
      sendLogMsgToTromboneActor()
    }

  def logMessagesWithStartAndEndTimer(): Unit = {
    val id         = RequestId()
    val startToken = time.start(id, startEndTimeTestRegionName)
    sendLogMsgToTromboneActor()
    time.end(id, startEndTimeTestRegionName, startToken)
  }

  // DEOPSCSW-142: Flexibility of logging approaches
  // DEOPSCSW-122: Allow local component logs to be output to STDOUT
  // DEOPSCSW-123: Allow local component logs to be output to a file
  test("should able to log messages to combination of standard out and file concurrently and also log time messages.") {

    logMessagesWithTimer()
    logMessagesWithStartAndEndTimer()
    Thread.sleep(10)

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
      log(LoggingKeys.NAME) shouldBe "TimingTest"
    }

    // validating file logger
    testLogBuffer(fileLogBuffer)
    // validating console logger
    testLogBuffer(logBuffer)

    def testLogBuffer(logBuffer: mutable.Buffer[JsonObject]): Unit = {
      logBuffer.foreach { log ⇒
        log.contains(LoggingKeys.COMPONENT_NAME) shouldBe true
        log.contains(LoggingKeys.ACTOR) shouldBe true
        log(LoggingKeys.COMPONENT_NAME) shouldBe IRIS_NAME
        log(LoggingKeys.ACTOR) shouldBe irisActorRef.path.toString
        log.contains(LoggingKeys.FILE) shouldBe true
        log.contains(LoggingKeys.LINE) shouldBe true
        log.contains(LoggingKeys.CLASS) shouldBe true
      }
    }
  }

}
