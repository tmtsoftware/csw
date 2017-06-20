package csw.services.logging.scaladsl

import com.persist.JsonOps
import csw.services.logging.internal.LoggingLevels
import csw.services.logging.internal.LoggingLevels._
import csw.services.logging.utils.LoggingTestSuite
import org.scalatest.prop.TableDrivenPropertyChecks._

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

class LoggingTest extends LoggingTestSuite {

  // DEOPSCSW-116: Make log messages identifiable with components
  test("component logs should contain component name") {
    new TromboneHcd().startLogging(logMsgMap)
    Thread.sleep(100)

    logBuffer.foreach { log ⇒
      log.contains("@componentName") shouldBe true
      log("@componentName") shouldBe "tromboneHcd"
    }

  }

  // DEOPSCSW-126 : Configurability of logging characteristics for component / log instance
  test("should load default filter provided in configuration file and applied to normal logging messages") {

    //  TromboneHcd component is logging 6 messages
    //  As per the filter, hcd should log 5 message of all the levels except TRACE
    new TromboneHcd().startLogging(logMsgMap)
    Thread.sleep(200)

    //  TromboneHcd component is logging 6 messages each of unique level
    //  As per the filter, hcd should log 5 message of all level except TRACE
    logBuffer.size shouldBe 5

    val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json("@componentName").toString)
    val tromboneHcdLogs          = groupByComponentNamesLog.get("tromboneHcd").get

    tromboneHcdLogs.size shouldBe 5

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    tromboneHcdLogs.toList.foreach { log ⇒
      val currentLogLevel = log("@severity").toString.toLowerCase
      val currentLogMsg   = log("message").toString
      Level(currentLogLevel) >= LoggingLevels.DEBUG shouldBe true
      currentLogMsg shouldBe logMsgMap.get(currentLogLevel).get
    }
  }

  // DEOPSCSW-126 : Configurability of logging characteristics for component / log instance
  test("should apply default log level provided in configuration file for normal logging messages") {

    new TromboneAssembly().startLogging(logMsgMap)
    Thread.sleep(200)

    //  TromboneAssembly component is logging 6 messages each of unique level
    //  As per the default loglevel = trace, assembly should log all 6 message
    logBuffer.size shouldBe 6

    val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json("@componentName").toString)
    val tromboneAssemblyLogs     = groupByComponentNamesLog.get("tromboneAssembly").get

    tromboneAssemblyLogs.size shouldBe 6

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    tromboneAssemblyLogs.toList.foreach { log ⇒
      val currentLogLevel = log("@severity").toString.toLowerCase
      val currentLogMsg   = log("message").toString
      Level(currentLogLevel) >= LoggingLevels.TRACE shouldBe true
      currentLogMsg shouldBe logMsgMap.get(currentLogLevel).get
    }
  }

  // DEOPSCSW-124: Define severity levels for log messages
  // DEOPSCSW-125: Define severity levels for specific components/log instances
  test("should able to filter logs based on configured/updated log level (covers all levels)") {
    val testData = Table(
      ("logLevel", "expectedLogCount"),
      (FATAL, 1),
      (ERROR, 2),
      (WARN, 3),
      (INFO, 4),
      (DEBUG, 5),
      (TRACE, 6)
    )
    val compName = "tromboneAssembly"

    def filterLogsByComponentName(compName: String): Seq[JsonOps.JsonObject] = {
      val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json("@componentName").toString)
      groupByComponentNamesLog.get(compName).get
    }

    forAll(testData) { (logLevel: Level, logCount: Int) =>
      loggingSystem.setLevel(logLevel)

      new TromboneAssembly().startLogging(logMsgMap)
      Thread.sleep(200)

      val tromboneAssemblyLogs = filterLogsByComponentName(compName)
      tromboneAssemblyLogs.size shouldBe logCount

      tromboneAssemblyLogs.toList.foreach { log ⇒
        val currentLogLevel = log("@severity").toString.toLowerCase
        val currentLogMsg   = log("message").toString
        Level(currentLogLevel) >= logLevel shouldBe true
        currentLogMsg shouldBe logMsgMap.get(currentLogLevel).get
      }

      logBuffer.clear()
    }

  }
}
