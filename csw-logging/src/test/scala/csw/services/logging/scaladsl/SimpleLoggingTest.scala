package csw.services.logging.scaladsl

import java.time._

import com.persist.JsonOps
import com.persist.JsonOps.JsonObject
import csw.services.logging.commons.TMTDateTimeFormatter
import csw.services.logging.components.{InnerSourceComponent, SingletonComponent, TromboneAssembly, TromboneHcd}
import csw.services.logging.internal.LoggingLevels
import csw.services.logging.internal.LoggingLevels._
import csw.services.logging.utils.LoggingTestSuite
import org.scalatest.prop.TableDrivenPropertyChecks._

class SimpleLoggingTest extends LoggingTestSuite {

  // DEOPSCSW-116: Make log messages identifiable with components
  // DEOPSCSW-118: Provide UTC time for each log message
  // DEOPSCSW-119: Associate source with each log message
  // DEOPSCSW-121: Define structured tags for log messages
  test("logs should contain component name and source location in terms of file name, class name and line number") {
    val expectedTimeMillis = Instant.now.toEpochMilli +- 50
    new TromboneHcd().startLogging(logMsgMap)
    Thread.sleep(100)

    // filter for tromboneHcd is at debug level in config
    var logMsgLineNumber = TromboneHcd.DEBUG_LINE_NO

    logBuffer.foreach { log ⇒
      val actualTimestamp  = TMTDateTimeFormatter.parse(log("timestamp").toString)
      val actualTimeMillis = actualTimestamp.atZone(ZoneId.systemDefault).toInstant.toEpochMilli

      actualTimeMillis shouldBe expectedTimeMillis

      log("@componentName") shouldBe "tromboneHcd"
      log("file") shouldBe "TromboneHcd.scala"
      log("line") shouldBe logMsgLineNumber
      log("class") shouldBe "csw.services.logging.components.TromboneHcd"
      logMsgLineNumber += 1
    }
  }

  // DEOPSCSW-116: Make log messages identifiable with components
  // DEOPSCSW-119: Associate source with each log message
  // DEOPSCSW-121: Define structured tags for log messages
  test("inner class logs should contain source location in terms of file name, class name and line number") {
    new InnerSourceComponent().startLogging(logMsgMap)
    Thread.sleep(100)

    // default log level is TRACE in config
    var logMsgLineNumber = InnerSourceComponent.TRACE_LINE_NO

    logBuffer.foreach { log ⇒
      log("@componentName") shouldBe "InnerSourceComponent"
      log("file") shouldBe "InnerSourceComponent.scala"
      log("line") shouldBe logMsgLineNumber
      log("class") shouldBe "csw.services.logging.components.InnerSourceComponent$InnerSource"
      logMsgLineNumber += 1
    }
  }

  // DEOPSCSW-116: Make log messages identifiable with components
  // DEOPSCSW-119: Associate source with each log message
  // DEOPSCSW-121: Define structured tags for log messages
  test("singleton object logs should contain source location in terms of file name, class name and line number") {
    SingletonComponent.startLogging(logMsgMap)
    Thread.sleep(100)

    // default log level is TRACE in config
    var logMsgLineNumber = SingletonComponent.TRACE_LINE_NO

    logBuffer.foreach { log ⇒
      log.contains("@componentName") shouldBe true
      log("@componentName") shouldBe "SingletonComponent"
      log("file") shouldBe "SingletonComponent.scala"
      log("line") shouldBe logMsgLineNumber
      log("class") shouldBe "csw.services.logging.components.SingletonComponent"
      logMsgLineNumber += 1
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
    val tromboneHcdLogs          = groupByComponentNamesLog("tromboneHcd")

    tromboneHcdLogs.size shouldBe 5

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    tromboneHcdLogs.toList.foreach { log ⇒
      val currentLogLevel = log("@severity").toString.toLowerCase
      val currentLogMsg   = log("message").toString
      Level(currentLogLevel) >= LoggingLevels.DEBUG shouldBe true
      currentLogMsg shouldBe logMsgMap(currentLogLevel)
    }
  }

  // DEOPSCSW-126 : Configurability of logging characteristics for component / log instance
  test("should apply default log level provided in configuration file for normal logging messages") {

    new TromboneAssembly().startLogging(logMsgMap)
    Thread.sleep(300)

    //  TromboneAssembly component is logging 6 messages each of unique level
    //  As per the default loglevel = trace, assembly should log all 6 message
    logBuffer.size shouldBe 6

    val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json("@componentName").toString)
    val tromboneAssemblyLogs     = groupByComponentNamesLog("tromboneAssembly")

    tromboneAssemblyLogs.size shouldBe 6

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    tromboneAssemblyLogs.toList.foreach { log ⇒
      val currentLogLevel = log("@severity").toString.toLowerCase
      val currentLogMsg   = log("message").toString
      Level(currentLogLevel) >= LoggingLevels.TRACE shouldBe true
      currentLogMsg shouldBe logMsgMap(currentLogLevel)
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
      groupByComponentNamesLog(compName)
    }

    val defaultLogLevel = loggingSystem.getDefaultLogLevel

    forAll(testData) { (logLevel: Level, logCount: Int) =>
      loggingSystem.setComponentLogLevel(compName, logLevel)

      // confirm default log level is not changing when we change the component log level
      loggingSystem.getDefaultLogLevel shouldBe defaultLogLevel

      new TromboneAssembly().startLogging(logMsgMap)
      Thread.sleep(200)

      val tromboneAssemblyLogs = filterLogsByComponentName(compName)
      tromboneAssemblyLogs.size shouldBe logCount

      tromboneAssemblyLogs.toList.foreach { log ⇒
        val currentLogLevel = log("@severity").toString.toLowerCase
        val currentLogMsg   = log("message").toString
        Level(currentLogLevel) >= logLevel shouldBe true
        currentLogMsg shouldBe logMsgMap(currentLogLevel)
      }

      logBuffer.clear()
    }
  }

  // DEOPSCSW-124: Define severity levels for log messages
  // DEOPSCSW-125: Define severity levels for specific components/log instances
  // This test is identical to above test, except uses a TromboneHcd, which differs from the TromboneAssembly
  // because it gets its default log level from the configuration file.
  test("should able to filter logs based on configured/updated log level (covers all levels) with non-default level") {
    val testData = Table(
      ("logLevel", "expectedLogCount"),
      (FATAL, 1),
      (ERROR, 2),
      (WARN, 3),
      (INFO, 4),
      (DEBUG, 5),
      (TRACE, 6)
    )
    val compName = TromboneHcd.NAME

    def filterLogsByComponentName(compName: String): Seq[JsonOps.JsonObject] = {
      val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json("@componentName").toString)
      groupByComponentNamesLog(compName)
    }

    val defaultLogLevel = loggingSystem.getDefaultLogLevel

    forAll(testData) { (logLevel: Level, logCount: Int) =>
      loggingSystem.setComponentLogLevel(compName, logLevel)

      // confirm default log level is not changing when we change the component log level
      loggingSystem.getDefaultLogLevel shouldBe defaultLogLevel

      new TromboneHcd().startLogging(logMsgMap)
      Thread.sleep(200)

      val tromboneHcdLogs = filterLogsByComponentName(compName)
      tromboneHcdLogs.size shouldBe logCount

      tromboneHcdLogs.toList.foreach { log ⇒
        val currentLogLevel = log("@severity").toString.toLowerCase
        val currentLogMsg   = log("message").toString
        Level(currentLogLevel) >= logLevel shouldBe true
        currentLogMsg shouldBe logMsgMap(currentLogLevel)
      }

      logBuffer.clear()
    }
  }

  test("alternative log message should contain @category") {
    new TromboneHcd().startLogging(logMsgMap, "alternative")
    Thread.sleep(100)

    logBuffer.foreach { log ⇒
      log("@category") shouldBe true
    }
  }

  test("should able to log exception at ERROR level with complete stacktrace") {
    val tromboneHcd          = new TromboneHcd()
    val tromboneHcdClassName = tromboneHcd.getClass.getName

    val computationResultMsg = tromboneHcd.compute(10, 0)
    tromboneHcd.logRichException(computationResultMsg)
    Thread.sleep(300)

    /*
     * sample error log json looks like this, assertions are based on this structure.
     * {
     *   "@componentName":"tromboneHcd",
     *   "@severity":"ERROR",
     *   "class":"csw.services.logging.scaladsl.TromboneHcd",
     *   "file":"LoggingTest.scala",
     *   "line":37,
     *   "message":"Arithmetic Exception occurred.",
     *   "timestamp":"2017-06-30T12:21:39.784000000+05:30",
     *   "trace":{
     *      "message":{
     *         "ex":"class java.lang.ArithmeticException",
     *         "message":"/ by zero"
     *      },
     *      "stack":[ ... ]
     *   }
     * }
     */
    logBuffer.foreach { log ⇒
      log("@componentName") shouldBe "tromboneHcd"
      log("@severity") shouldBe ERROR.name
      log("class") shouldBe tromboneHcdClassName
      log("message") shouldBe computationResultMsg

      log.contains("trace") shouldBe true
      val traceBlock = log("trace").asInstanceOf[JsonObject]
      traceBlock.contains("message") shouldBe true
      traceBlock.contains("stack") shouldBe true

      val traceMsgBlock = traceBlock("message").asInstanceOf[JsonObject]
      traceMsgBlock.contains("ex") shouldBe true
      traceMsgBlock.contains("message") shouldBe true
    }
  }

}
