package csw.logging.internal

import java.nio.file.Paths
import java.time.{ZoneId, ZoneOffset, ZonedDateTime}

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.logging.appenders.FileAppender
import csw.logging.commons.LoggingKeys
import csw.logging.components.IRIS
import csw.logging.components.IRIS._
import csw.logging.internal.JsonExtensions.RichJsObject
import csw.logging.scaladsl.RequestId
import csw.logging.utils.{FileUtils, LoggingTestSuite}
import play.api.libs.json.{JsArray, JsObject, Json}

import scala.collection.mutable

class TimingTest extends LoggingTestSuite with Timing {

  private val logFileDir = Paths.get("/tmp/csw-test-logs").toFile
  private val config = ConfigFactory
    .parseString(s"csw-logging.appender-config.file.logPath=${logFileDir.getAbsolutePath}")
    .withFallback(ConfigFactory.load())

  private val loggingSystemName = "TimingTest"
  override lazy val actorSystem = ActorSystem("timing-test-system", config)
  override lazy val loggingSystem =
    new LoggingSystem(loggingSystemName, "version", "localhost", actorSystem)

  private val irisActorRef =
    actorSystem.actorOf(IRIS.props(IRIS.COMPONENT_NAME), name = "IRIS-Supervisor-Actor")

  private val fileTimestamp   = FileAppender.decideTimestampForFile(ZonedDateTime.now(ZoneId.from(ZoneOffset.UTC)))
  private val fullLogFileDir  = logFileDir + "/" + loggingSystemName
  private val timeLogFilePath = fullLogFileDir + s"/time.$fileTimestamp.log"
  private val testLogFilePath = fullLogFileDir + s"/common.$fileTimestamp.log"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    FileUtils.deleteRecursively(logFileDir)
    loggingSystem.setAppenders(List(testAppender, FileAppender))
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    FileUtils.deleteRecursively(logFileDir)
  }

  def sendLogMsgToIRISActor(): Unit = {
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
      sendLogMsgToIRISActor()
    }

  def logMessagesWithStartAndEndTimer(): Unit = {
    val id         = RequestId()
    val startToken = time.start(id, startEndTimeTestRegionName)
    sendLogMsgToIRISActor()
    time.end(id, startEndTimeTestRegionName, startToken)
  }

  // DEOPSCSW-142: Flexibility of logging approaches
  // DEOPSCSW-122: Allow local component logs to be output to STDOUT
  // DEOPSCSW-123: Allow local component logs to be output to a file
  test("should able to log messages to combination of standard out and file concurrently and also log time messages.") {

    logMessagesWithTimer()
    logMessagesWithStartAndEndTimer()
    Thread.sleep(300)

    // Reading time logger file
    val timeLogBuffer = FileUtils.read(timeLogFilePath)
    // Reading common logger file
    val fileLogBuffer = FileUtils.read(testLogFilePath)

    // validating timer logger
    timeLogBuffer.toList.foreach { log ⇒
      val itemsMap = log("items").as[List[JsObject]].head

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

    def testLogBuffer(logBuffer: mutable.Buffer[JsObject]): Unit = {
      logBuffer.foreach { log ⇒
        val currentLogLevel = log(LoggingKeys.SEVERITY).toString.toLowerCase
        log(LoggingKeys.MESSAGE).toString shouldBe IRIS.irisLogs(currentLogLevel)

        log(LoggingKeys.COMPONENT_NAME) shouldBe IRIS.COMPONENT_NAME
        log(LoggingKeys.ACTOR) shouldBe irisActorRef.path.toString
        log(LoggingKeys.FILE) shouldBe IRIS.FILE_NAME
        log(LoggingKeys.CLASS) shouldBe IRIS.CLASS_NAME
        log.contains(LoggingKeys.LINE) shouldBe true
      }
    }
  }

}
