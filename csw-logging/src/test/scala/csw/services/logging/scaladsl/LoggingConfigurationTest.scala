package csw.services.logging.scaladsl

import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import java.time.{ZoneId, ZoneOffset, ZonedDateTime}

import akka.actor.ActorSystem
import com.persist.JsonOps
import com.persist.JsonOps.JsonObject
import com.typesafe.config.ConfigFactory
import csw.services.logging.appenders.FileAppender
import csw.services.logging.commons.LoggingKeys
import csw.services.logging.internal.LoggingLevels.INFO
import csw.services.logging.utils.FileUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

// DEOPSCSW-126: Configurability of logging characteristics for component / log instance
// DEOPSCSW-142: Flexibility of logging approaches
class LoggingConfigurationTest
    extends FunSuite
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with GenericLogger.Simple {

  private val logFileDir                     = Paths.get("/tmp/csw-test-logs").toFile
  private val sampleLogMessage               = "Sample log message"
  private val fileTimestamp                  = FileAppender.decideTimestampForFile(ZonedDateTime.now(ZoneId.from(ZoneOffset.UTC)))
  private val loggingSystemName              = "Test"
  private val testLogFilePathWithServiceName = logFileDir + "/" + loggingSystemName + s"/common.$fileTimestamp.log"

  private val hostname  = "localhost"
  private val version   = "SNAPSHOT-1.0"
  private val className = getClass.getName
  private val fileName  = "LoggingConfigurationTest.scala"

  private val outStream       = new ByteArrayOutputStream
  private val stdOutLogBuffer = mutable.Buffer.empty[JsonObject]

  override protected def beforeAll(): Unit = {
    FileUtils.deleteRecursively(logFileDir)
  }

  override protected def afterEach(): Unit = {
    stdOutLogBuffer.clear()
    outStream.reset()
    FileUtils.deleteRecursively(logFileDir)
  }

  override protected def afterAll(): Unit = outStream.close()

  def parse(str: String): Unit = {
    str.split("\n").foreach { log ⇒
      stdOutLogBuffer += JsonOps.Json(log).asInstanceOf[Map[String, String]]
    }
  }

  test("should log messages in the file with standard headers") {
    val config =
      ConfigFactory
        .parseString(s"""
                        |csw-logging {
                        | appenders = ["csw.services.logging.appenders.FileAppender$$"]
                        | appender-config {
                        |   file {
                        |     fullHeaders = true
                        |     logPath = ${logFileDir.getAbsolutePath}
                        |   }
                        | }
                        |}
                      """.stripMargin)
        .withFallback(ConfigFactory.load)

    val actorSystem   = ActorSystem("test", config)
    val loggingSystem = LoggingSystemFactory.start(loggingSystemName, version, hostname, actorSystem)

    log.info(sampleLogMessage)
    Thread.sleep(100)

    // Reading common logger file
    val fileLogBuffer = FileUtils.read(testLogFilePathWithServiceName)

    fileLogBuffer.size shouldBe 1
    val jsonLogMessage = fileLogBuffer.head

    // Standard header fields -> Logging System Name, Hostname, Version
    jsonLogMessage(LoggingKeys.NAME) shouldBe loggingSystemName
    jsonLogMessage(LoggingKeys.HOST) shouldBe hostname
    jsonLogMessage(LoggingKeys.VERSION) shouldBe version

    jsonLogMessage(LoggingKeys.MESSAGE) shouldBe sampleLogMessage
    jsonLogMessage(LoggingKeys.SEVERITY) shouldBe INFO.name
    jsonLogMessage(LoggingKeys.CLASS) shouldBe className
    jsonLogMessage(LoggingKeys.FILE) shouldBe fileName

    // clean up
    Await.result(loggingSystem.stop, 5.seconds)
    Await.result(actorSystem.terminate, 5.seconds)
  }

  test("should log messages in the file without standard headers based on the log level configured in the config") {
    val config =
      ConfigFactory
        .parseString(s"""
                        |csw-logging {
                        | appenders = ["csw.services.logging.appenders.FileAppender$$"]
                        | appender-config {
                        |   file {
                        |     fullHeaders = false
                        |     logPath = ${logFileDir.getAbsolutePath}
                        |     logLevelLimit = info
                        |   }
                        | }
                        |}
                      """.stripMargin)
        .withFallback(ConfigFactory.load)

    val actorSystem   = ActorSystem("test", config)
    val loggingSystem = LoggingSystemFactory.start(loggingSystemName, version, hostname, actorSystem)

    log.info(sampleLogMessage)
    log.debug(sampleLogMessage)
    Thread.sleep(200)

    // Reading common logger file
    val fileLogBuffer = FileUtils.read(testLogFilePathWithServiceName)

    // LogLevelLimit configured in file appender is INFO, hence only one message with the level info should be logged
    fileLogBuffer.size shouldBe 1
    val jsonLogMessage = fileLogBuffer.head

    // Standard header fields -> Logging System Name, Hostname, Version
    jsonLogMessage.contains(LoggingKeys.NAME) shouldBe false
    jsonLogMessage.contains(LoggingKeys.HOST) shouldBe false
    jsonLogMessage.contains(LoggingKeys.VERSION) shouldBe false

    jsonLogMessage(LoggingKeys.MESSAGE) shouldBe sampleLogMessage
    jsonLogMessage(LoggingKeys.SEVERITY) shouldBe INFO.name
    jsonLogMessage(LoggingKeys.CLASS) shouldBe className
    jsonLogMessage(LoggingKeys.FILE) shouldBe fileName

    // clean up
    Await.result(loggingSystem.stop, 5.seconds)
    Await.result(actorSystem.terminate, 5.seconds)
  }

  test("should log messages on the console with standard headers based on the log level configured in the config") {
    val config =
      ConfigFactory
        .parseString("""
                        |csw-logging {
                        | appenders = ["csw.services.logging.appenders.StdOutAppender$"]
                        | appender-config {
                        |   stdout {
                        |     fullHeaders = true
                        |     logLevelLimit = info
                        |   }
                        | }
                        |}
                      """.stripMargin)
        .withFallback(ConfigFactory.load)

    lazy val actorSystem   = ActorSystem("test", config)
    lazy val loggingSystem = LoggingSystemFactory.start(loggingSystemName, version, hostname, actorSystem)

    Console.withOut(outStream) {
      loggingSystem
      log.debug(sampleLogMessage)
      log.info(sampleLogMessage)
      Thread.sleep(200)
    }

    parse(outStream.toString)

    // LogLevelLimit configured in stdout appender is INFO, hence only one message with the level info should be logged
    stdOutLogBuffer.size shouldBe 1
    val jsonLogMessage = stdOutLogBuffer.head

    // Standard header fields -> Logging System Name, Hostname, Version
    jsonLogMessage(LoggingKeys.NAME) shouldBe loggingSystemName
    jsonLogMessage(LoggingKeys.HOST) shouldBe hostname
    jsonLogMessage(LoggingKeys.VERSION) shouldBe version

    jsonLogMessage(LoggingKeys.MESSAGE) shouldBe sampleLogMessage
    jsonLogMessage(LoggingKeys.SEVERITY) shouldBe INFO.name
    jsonLogMessage(LoggingKeys.CLASS) shouldBe className
    jsonLogMessage(LoggingKeys.FILE) shouldBe fileName

    // clean up
    Await.result(loggingSystem.stop, 5.seconds)
    Await.result(actorSystem.terminate, 5.seconds)
  }

  test("should log messages on the console without standard headers") {
    val config =
      ConfigFactory
        .parseString("""
                       |csw-logging {
                       | appenders = ["csw.services.logging.appenders.StdOutAppender$"]
                       | appender-config {
                       |   stdout {
                       |     fullHeaders = false
                       |   }
                       | }
                       |}
                     """.stripMargin)
        .withFallback(ConfigFactory.load)

    lazy val actorSystem   = ActorSystem("test", config)
    lazy val loggingSystem = LoggingSystemFactory.start(loggingSystemName, version, hostname, actorSystem)

    Console.withOut(outStream) {
      loggingSystem
      log.info(sampleLogMessage)
      Thread.sleep(100)
    }

    parse(outStream.toString)

    stdOutLogBuffer.size shouldBe 1
    val jsonLogMessage = stdOutLogBuffer.head

    // Standard header fields -> Logging System Name, Hostname, Version
    jsonLogMessage.contains(LoggingKeys.NAME) shouldBe false
    jsonLogMessage.contains(LoggingKeys.HOST) shouldBe false
    jsonLogMessage.contains(LoggingKeys.VERSION) shouldBe false

    jsonLogMessage(LoggingKeys.MESSAGE) shouldBe sampleLogMessage
    jsonLogMessage(LoggingKeys.SEVERITY) shouldBe INFO.name
    jsonLogMessage(LoggingKeys.CLASS) shouldBe className
    jsonLogMessage(LoggingKeys.FILE) shouldBe fileName

    // clean up
    stdOutLogBuffer.clear()
    Await.result(loggingSystem.stop, 5.seconds)
    Await.result(actorSystem.terminate, 5.seconds)
  }

  test("should log messages on the console in one line 1") {
    val os = new ByteArrayOutputStream
    val config =
      ConfigFactory
        .parseString("""
                       |csw-logging {
                       | appenders = ["csw.services.logging.appenders.StdOutAppender$"]
                       | appender-config {
                       |   stdout {
                       |     oneLine = true
                       |   }
                       | }
                       |}
                     """.stripMargin)
        .withFallback(ConfigFactory.load)

    lazy val loggingSystem = LoggingSystemFactory.start(loggingSystemName, version, hostname, actorSystem)
    lazy val actorSystem   = ActorSystem("test", config)

    Console.withOut(os) {
      loggingSystem
      log.info(sampleLogMessage)
      Thread.sleep(100)
    }

    val expectedOneLineLog = "[INFO] Sample log message (LoggingConfigurationTest.scala 264)"

    os.toString.trim shouldBe expectedOneLineLog

    // clean up
    os.flush()
    os.close()
    Await.result(loggingSystem.stop, 5.seconds)
    Await.result(actorSystem.terminate, 5.seconds)
  }
}
