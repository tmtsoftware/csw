package csw.services.logging.scaladsl

import java.net.InetAddress

import com.persist.JsonOps._
import csw.services.logging.internal.LoggingLevels
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.utils.TestAppender
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

object TromboneHcdLogger      extends ComponentLogger("tromboneHcd")
object TromboneAssemblyLogger extends ComponentLogger("tromboneAssembly")

class TromboneHcd() extends TromboneHcdLogger.Simple {
  def startLogging(logs: Map[String, String]): Unit = {
    log.trace(logs.get("trace").get)
    log.debug(logs.get("debug").get)
    log.info(logs.get("info").get)
    log.warn(logs.get("warn").get)
    log.error(logs.get("error").get)
    log.fatal(logs.get("fatal").get)
  }
}

class TromboneAssembly() extends TromboneAssemblyLogger.Simple {
  def startLogging(logs: Map[String, String]): Unit = {
    log.trace(logs.get("trace").get)
    log.debug(logs.get("debug").get)
    log.info(logs.get("info").get)
    log.warn(logs.get("warn").get)
    log.error(logs.get("error").get)
    log.fatal(logs.get("fatal").get)
  }
}

class LoggerTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  private val logBuffer    = mutable.Buffer.empty[JsonObject]
  private val testAppender = new TestAppender(x ⇒ logBuffer += Json(x.toString).asInstanceOf[JsonObject])

  private val hostName      = InetAddress.getLocalHost.getHostName
  private val loggingSystem = new LoggingSystem("logging", "SNAPSHOT-1.0", hostName, Seq(testAppender))

  private val logMsgMap = Map(
    "trace" → "logging at trace level",
    "debug" → "logging at debug level",
    "info"  → "logging at info level",
    "warn"  → "logging at warn level",
    "error" → "logging at error level",
    "fatal" → "logging at fatal level"
  )

  override protected def afterEach(): Unit = logBuffer.clear()

  override protected def afterAll(): Unit = Await.result(loggingSystem.stop, 30.seconds)

  test("component logs should contain component name") {
    new TromboneHcd().startLogging(logMsgMap)
    Thread.sleep(100)

    logBuffer.foreach { log ⇒
      log.contains("@componentName") shouldBe true
    }

  }

  test("should load default filter provided in configuration file") {

    /*
      --------------------------------------
      default filters for components are =>
      tromboneHcd = debug
      tromboneAssembly = error
      --------------------------------------
     */

    new TromboneHcd().startLogging(logMsgMap)
    new TromboneAssembly().startLogging(logMsgMap)
    Thread.sleep(100)

    //  TromboneHcd component is logging 6 messages each of unique level
    //  TromboneAssembly component is logging 6 messages each of unique level
    //  As per the filter, hcd should log 5 message of all level except TRACE
    //  As per the filter, assembly should log 2 message of level (ERROR & FATAL)
    logBuffer.size shouldBe 7

    val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json("@componentName").toString)
    val tromboneHcdLogs          = groupByComponentNamesLog.get("tromboneHcd").get
    val tromboneAssemblyLogs     = groupByComponentNamesLog.get("tromboneAssembly").get

    tromboneHcdLogs.size shouldBe 5
    tromboneAssemblyLogs.size shouldBe 2

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    tromboneHcdLogs.toList.foreach { log ⇒
      val currentLogLevel = log("@severity").toString.toLowerCase
      val currentLogMsg   = log("msg").toString
      Level(currentLogLevel) >= LoggingLevels.DEBUG shouldBe true
      currentLogMsg shouldBe logMsgMap.get(currentLogLevel).get
    }

    // check that log level should be greater than or equal to error and
    // assert on actual log message
    tromboneAssemblyLogs.toList.foreach { log ⇒
      val currentLogLevel = log("@severity").toString.toLowerCase
      val currentLogMsg   = log("msg").toString
      Level(currentLogLevel) >= LoggingLevels.ERROR shouldBe true
      currentLogMsg shouldBe logMsgMap.get(currentLogLevel).get
    }
  }
}
