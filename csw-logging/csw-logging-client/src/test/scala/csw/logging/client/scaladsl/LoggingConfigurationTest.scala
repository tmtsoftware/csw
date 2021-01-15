package csw.logging.client.scaladsl

import java.io.ByteArrayOutputStream
import java.nio.file.{Files, Paths}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{ZoneId, ZoneOffset, ZonedDateTime}

import akka.actor.typed
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.appenders.{FileAppender, StdOutAppender}
import csw.logging.client.commons.{LoggingKeys, TMTDateTimeFormatter}
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.utils.FileUtils
import csw.logging.models.Level
import csw.logging.models.Level.{DEBUG, ERROR, INFO, TRACE}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationLong
import scala.reflect.io.File

// DEOPSCSW-122: Allow local component logs to be output to STDOUT
// DEOPSCSW-123: Allow local component logs to be output to a file
// DEOPSCSW-126: Configurability of logging characteristics for component / log instance
// DEOPSCSW-142: Flexibility of logging approaches
// DEOPSCSW-649: Fixed directory configuration for multi JVM scenario
class LoggingConfigurationTest extends AnyFunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {
  val log: Logger                            = GenericLoggerFactory.getLogger
  private val logFileDir                     = Paths.get("/tmp/csw-test-logs").toFile
  private val sampleLogMessage               = "Sample log message"
  private val fileTimestamp                  = FileAppender.decideTimestampForFile(ZonedDateTime.now(ZoneId.from(ZoneOffset.UTC)))
  private val loggingSystemName              = "Test"
  private val testLogFilePathWithServiceName = s"$logFileDir/${loggingSystemName}_$fileTimestamp.log"

  private val hostname  = "localhost"
  private val version   = "SNAPSHOT-1.0"
  private val className = getClass.getName
  private val fileName  = "LoggingConfigurationTest.scala"

  private val outStream       = new ByteArrayOutputStream
  private val stdOutLogBuffer = mutable.Buffer.empty[JsObject]

  override protected def beforeAll(): Unit = {
    FileUtils.deleteRecursively(logFileDir)
    File(logFileDir).createDirectory()
    File(testLogFilePathWithServiceName).createFile()
  }

  override protected def afterEach(): Unit = {
    stdOutLogBuffer.clear()
    outStream.reset()
    FileUtils.deleteRecursively(logFileDir)
  }

  override protected def afterAll(): Unit = outStream.close()

  def parse(str: String): Unit = {
    str.split("\n").foreach { log => stdOutLogBuffer += Json.parse(log).as[JsObject] }
  }

  def testLogConfiguration(
      logBuffer: mutable.Seq[JsObject],
      headersEnabled: Boolean,
      expectedTimestamp: ZonedDateTime,
      expectedSize: Int = 1,
      expectedSeverity: Level = INFO
  ): Unit = {
    logBuffer.size shouldBe expectedSize

    logBuffer.foreach { jsonLogMessage =>
      // Standard header fields -> Logging System Name, Hostname, Version
      jsonLogMessage.contains(LoggingKeys.NAME) shouldBe headersEnabled
      jsonLogMessage.contains(LoggingKeys.HOST) shouldBe headersEnabled
      jsonLogMessage.contains(LoggingKeys.VERSION) shouldBe headersEnabled

      if (headersEnabled) {
        jsonLogMessage.getString(LoggingKeys.NAME) shouldBe loggingSystemName
        jsonLogMessage.getString(LoggingKeys.HOST) shouldBe hostname
        jsonLogMessage.getString(LoggingKeys.VERSION) shouldBe version
      }

      jsonLogMessage.getString(LoggingKeys.MESSAGE) shouldBe sampleLogMessage
      jsonLogMessage.getString(LoggingKeys.SEVERITY) shouldBe expectedSeverity.name
      jsonLogMessage.getString(LoggingKeys.CLASS) shouldBe className
      jsonLogMessage.getString(LoggingKeys.FILE) shouldBe fileName

      // This assert's that, ISO_INSTANT parser should not throw exception while parsing timestamp from log message
      // If timestamp is in other than UTC(ISO_FORMAT) format, DateTimeFormatter.ISO_INSTANT will throw DateTimeParseException
      noException shouldBe thrownBy(DateTimeFormatter.ISO_INSTANT.parse(jsonLogMessage.getString(LoggingKeys.TIMESTAMP)))
      val actualDateTime = TMTDateTimeFormatter.parse(jsonLogMessage.getString(LoggingKeys.TIMESTAMP))
      ChronoUnit.MILLIS.between(expectedTimestamp, actualDateTime) <= 50 shouldBe true
    }

  }

  private def doLogging(): Unit = {
    log.info(sampleLogMessage)
    log.debug(sampleLogMessage)
  }

  // DEOPSCSW-118: Provide UTC time for each log message
  test(
    "should log messages in the file with standard headers | DEOPSCSW-122, DEOPSCSW-649, DEOPSCSW-118, DEOPSCSW-123, DEOPSCSW-126, DEOPSCSW-142"
  ) {
    val config =
      ConfigFactory
        .parseString(s"""
                        |csw-logging {
                        | appenders = ["csw.logging.client.appenders.FileAppender$$"]
                        | appender-config {
                        |   file {
                        |     fullHeaders = true
                        |     logLevelLimit = info
                        |   }
                        | }
                        |}
                      """.stripMargin)
        .withFallback(ConfigFactory.load())

    val actorSystem   = ActorSystem(SpawnProtocol(), "test", config)
    val loggingSystem = LoggingSystemFactory.start(loggingSystemName, version, hostname, actorSystem)

    loggingSystem.getAppenders shouldBe List(FileAppender)

    val expectedTimestamp = ZonedDateTime.now(ZoneId.from(ZoneOffset.UTC))

    doLogging()
    Thread.sleep(300)

    // Reading common logger file
    val fileLogBuffer = FileUtils.read(testLogFilePathWithServiceName)

    testLogConfiguration(fileLogBuffer, headersEnabled = true, expectedTimestamp)

    // clean up
    Await.result(loggingSystem.stop, 5.seconds)
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 5.seconds)
  }

  test(
    "should log messages in the file without standard headers based on the log level configured in the config | DEOPSCSW-122, DEOPSCSW-649, DEOPSCSW-123, DEOPSCSW-126, DEOPSCSW-142"
  ) {
    val config =
      ConfigFactory
        .parseString(s"""
                        |csw-logging {
                        | appenders = ["csw.logging.client.appenders.FileAppender$$"]
                        | appender-config {
                        |   file {
                        |     fullHeaders = false
                        |     logLevelLimit = info
                        |   }
                        | }
                        |}
                      """.stripMargin)
        .withFallback(ConfigFactory.load())

    val actorSystem   = typed.ActorSystem(SpawnProtocol(), "test", config)
    val loggingSystem = LoggingSystemFactory.start(loggingSystemName, version, hostname, actorSystem)

    loggingSystem.getAppenders shouldBe List(FileAppender)

    val expectedTimestamp = ZonedDateTime.now(ZoneId.from(ZoneOffset.UTC))
    doLogging()
    Thread.sleep(200)

    // Reading common logger file
    val fileLogBuffer = FileUtils.read(testLogFilePathWithServiceName)

    testLogConfiguration(fileLogBuffer, false, expectedTimestamp)

    // clean up
    Await.result(loggingSystem.stop, 5.seconds)
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 5.seconds)
  }

  test(
    "should log messages in the file and on console based on the log level configured in the file and stdout appender config | DEOPSCSW-122, DEOPSCSW-649, DEOPSCSW-123, DEOPSCSW-126, DEOPSCSW-142"
  ) {
    val config =
      ConfigFactory
        .parseString(s"""
                        |csw-logging {
                        | appenders = ["csw.logging.client.appenders.FileAppender$$", "csw.logging.client.appenders.StdOutAppender$$"]
                        | appender-config {
                        |   file {
                        |     logLevelLimit = debug
                        |   }
                        |   stdout {
                        |     logLevelLimit = trace
                        |   }
                        | }
                        | logLevel = trace
                        |}
                      """.stripMargin)
        .withFallback(ConfigFactory.load())

    lazy val actorSystem   = typed.ActorSystem(SpawnProtocol(), "test", config)
    lazy val loggingSystem = LoggingSystemFactory.start(loggingSystemName, version, hostname, actorSystem)

    // default log level is trace but file appender is filtering logs at debug level, hence trace level log should not be written to file
    // log level for stdOut appender is trace, hence std out appender should log all messages
    Console.withOut(outStream) {
      loggingSystem
      log.trace(sampleLogMessage)
      log.debug(sampleLogMessage)
      Thread.sleep(200)
    }

    loggingSystem.getAppenders shouldBe List(FileAppender, StdOutAppender)

    //*************************** Start Testing File Logs *****************************************
    val fileLogBuffer = FileUtils.read(testLogFilePathWithServiceName)
    fileLogBuffer.size shouldBe 1

    val jsonFileLogMessage = fileLogBuffer.head
    jsonFileLogMessage.getString(LoggingKeys.MESSAGE) shouldBe sampleLogMessage
    jsonFileLogMessage.getString(LoggingKeys.SEVERITY) shouldBe DEBUG.name
    jsonFileLogMessage.getString(LoggingKeys.CLASS) shouldBe className
    jsonFileLogMessage.getString(LoggingKeys.FILE) shouldBe fileName
    //*************************** End Testing File Logs *********************************************

    //*************************** Start Testing StdOut Logs *****************************************
    println(outStream.toString)
    parse(outStream.toString)
    stdOutLogBuffer.size shouldBe 2

    val firstStdOutLogMessage = stdOutLogBuffer.head
    firstStdOutLogMessage.getString(LoggingKeys.MESSAGE) shouldBe sampleLogMessage
    firstStdOutLogMessage.getString(LoggingKeys.SEVERITY) shouldBe TRACE.name
    firstStdOutLogMessage.getString(LoggingKeys.CLASS) shouldBe className
    firstStdOutLogMessage.getString(LoggingKeys.FILE) shouldBe fileName

    val secondStdOutLogMessage = stdOutLogBuffer.tail.head
    secondStdOutLogMessage.getString(LoggingKeys.MESSAGE) shouldBe sampleLogMessage
    secondStdOutLogMessage.getString(LoggingKeys.SEVERITY) shouldBe DEBUG.name
    secondStdOutLogMessage.getString(LoggingKeys.CLASS) shouldBe className
    secondStdOutLogMessage.getString(LoggingKeys.FILE) shouldBe fileName
    //*************************** End Testing StdOut Logs *******************************************

    // clean up
    stdOutLogBuffer.clear()
    Await.result(loggingSystem.stop, 5.seconds)
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 5.seconds)
  }

  // DEOPSCSW-118: Provide UTC time for each log message
  test(
    "should log messages on the console with standard headers based on the log level configured in the config | DEOPSCSW-122, DEOPSCSW-649, DEOPSCSW-118, DEOPSCSW-123, DEOPSCSW-126, DEOPSCSW-142"
  ) {
    val config =
      ConfigFactory
        .parseString("""
                       |csw-logging {
                       | appenders = ["csw.logging.client.appenders.StdOutAppender$"]
                       | appender-config {
                       |   stdout {
                       |     fullHeaders = true
                       |     logLevelLimit = info
                       |   }
                       | }
                       |}
                     """.stripMargin)
        .withFallback(ConfigFactory.load())

    lazy val actorSystem                 = typed.ActorSystem(SpawnProtocol(), "test", config)
    lazy val loggingSystem               = LoggingSystemFactory.start(loggingSystemName, version, hostname, actorSystem)
    var expectedTimestamp: ZonedDateTime = null

    Console.withOut(outStream) {
      loggingSystem
      expectedTimestamp = ZonedDateTime.now(ZoneId.from(ZoneOffset.UTC))
      doLogging()
      Thread.sleep(200)
    }
    loggingSystem.getAppenders shouldBe List(StdOutAppender)

    parse(outStream.toString)
    testLogConfiguration(stdOutLogBuffer, true, expectedTimestamp)

    // clean up
    Await.result(loggingSystem.stop, 5.seconds)
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 5.seconds)
  }

  test(
    "should log messages on the console without standard headers | DEOPSCSW-122, DEOPSCSW-649, DEOPSCSW-123, DEOPSCSW-126, DEOPSCSW-142"
  ) {
    val config =
      ConfigFactory
        .parseString("""
                       |csw-logging {
                       | appenders = ["csw.logging.client.appenders.StdOutAppender$"]
                       | appender-config {
                       |   stdout {
                       |     fullHeaders = false
                       |     logLevelLimit = info
                       |   }
                       | }
                       | logLevel = warn
                       |}
                     """.stripMargin)
        .withFallback(ConfigFactory.load())

    lazy val actorSystem                 = typed.ActorSystem(SpawnProtocol(), "test", config)
    var loggingSystem: LoggingSystem     = null
    var expectedTimestamp: ZonedDateTime = null

    Console.withOut(outStream) {
      // we ignore all the messages before LoggingSystem starts
      log.error(sampleLogMessage)
      log.info(sampleLogMessage)
      loggingSystem = LoggingSystemFactory.start(loggingSystemName, version, hostname, actorSystem)
      expectedTimestamp = ZonedDateTime.now(ZoneId.from(ZoneOffset.UTC))
      log.error(sampleLogMessage)
      log.info(sampleLogMessage)
      Thread.sleep(100)
    }

    loggingSystem.getAppenders shouldBe List(StdOutAppender)

    parse(outStream.toString)
    testLogConfiguration(stdOutLogBuffer, false, expectedTimestamp, 1, ERROR)

    // clean up
    stdOutLogBuffer.clear()
    Await.result(loggingSystem.stop, 5.seconds)
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 5.seconds)
  }

  test("should log messages on the console in one line | DEOPSCSW-122, DEOPSCSW-649, DEOPSCSW-123, DEOPSCSW-126, DEOPSCSW-142") {
    val os = new ByteArrayOutputStream
    val config =
      ConfigFactory
        .parseString("""
                       |csw-logging {
                       | appenders = ["csw.logging.client.appenders.StdOutAppender$"]
                       | appender-config {
                       |   stdout {
                       |     oneLine = true
                       |     logLevelLimit = info
                       |   }
                       | }
                       |}
                     """.stripMargin)
        .withFallback(ConfigFactory.load())

    lazy val actorSystem                 = typed.ActorSystem(SpawnProtocol(), "test", config)
    lazy val loggingSystem               = LoggingSystemFactory.start(loggingSystemName, version, hostname, actorSystem)
    var expectedTimestamp: ZonedDateTime = null

    Console.withOut(os) {
      loggingSystem
      expectedTimestamp = ZonedDateTime.now(ZoneId.from(ZoneOffset.UTC))
      doLogging()
      Thread.sleep(300)
    }
    loggingSystem.getAppenders shouldBe List(StdOutAppender)

    val expectedOneLineLog = " INFO   (LoggingConfigurationTest.scala 105) - Sample log message"

    val (timestamp, message) = os.toString.trim.splitAt(24)

    message shouldBe expectedOneLineLog

    // This assert's that, ISO_INSTANT parser should not throw exception while parsing timestamp from log message
    // If timestamp is in other than UTC(ISO_FORMAT) format, DateTimeFormatter.ISO_INSTANT will throw DateTimeParseException
    noException shouldBe thrownBy(DateTimeFormatter.ISO_INSTANT.parse(timestamp))
    val actualDateTime = TMTDateTimeFormatter.parse(timestamp)
    ChronoUnit.MILLIS.between(expectedTimestamp, actualDateTime) <= 50 shouldBe true

    // clean up
    os.flush()
    os.close()
    Await.result(loggingSystem.stop, 5.seconds)
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 5.seconds)
  }
}
