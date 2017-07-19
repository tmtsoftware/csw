package csw.services.logging.scaladsl

import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import com.persist.JsonOps
import com.persist.JsonOps.JsonObject
import csw.services.logging.commons.{Constants, LoggingKeys, TMTDateTimeFormatter}
import csw.services.logging.components.{InnerSourceComponent, SingletonComponent, TromboneAssembly, TromboneHcd}
import csw.services.logging.internal.LoggingLevels._
import csw.services.logging.internal.{LoggingLevels, LoggingState}
import csw.services.logging.utils.LoggingTestSuite
import org.scalatest.prop.TableDrivenPropertyChecks._

class SimpleLoggingTest extends LoggingTestSuite {

  // DEOPSCSW-116: Make log messages identifiable with components
  // DEOPSCSW-118: Provide UTC time for each log message
  // DEOPSCSW-119: Associate source with each log message
  // DEOPSCSW-121: Define structured tags for log messages
  test("logs should contain component name and source location in terms of file name, class name and line number") {
    val expectedDateTime = ZonedDateTime.now(ZoneId.from(ZoneOffset.UTC))
    new TromboneHcd().startLogging(logMsgMap)
    Thread.sleep(100)

    // Verify log level for tromboneHcd is at debug level in config
    LoggingState.componentsLoggingState(TromboneHcd.COMPONENT_NAME).componentLogLevel shouldBe LoggingLevels.DEBUG

    var logMsgLineNumber = TromboneHcd.DEBUG_LINE_NO

    logBuffer.foreach { log ⇒
      // This assert's that, ISO_INSTANT parser should not throw exception while parsing timestamp from log message
      // If timestamp is in other than UTC(ISO_FORMAT) format, DateTimeFormatter.ISO_INSTANT will throw DateTimeParseException
      // Ex.  2017-07-19T13:23:55.358+03:00 :=> DateTimeFormatter.ISO_INSTANT.parse will throw exception
      //      2017-07-19T01:23:55.360+05:30 :=> DateTimeFormatter.ISO_INSTANT.parse will throw exception
      //      2017-07-19T01:23:55.360Z      :=> DateTimeFormatter.ISO_INSTANT.parse will not throw exception
      noException shouldBe thrownBy(DateTimeFormatter.ISO_INSTANT.parse(log(LoggingKeys.TIMESTAMP).toString))

      val actualDateTime = TMTDateTimeFormatter.parse(log(LoggingKeys.TIMESTAMP).toString)
      ChronoUnit.MILLIS.between(expectedDateTime, actualDateTime) <= 50 shouldBe true

      log(LoggingKeys.COMPONENT_NAME) shouldBe "tromboneHcd"
      log(LoggingKeys.FILE) shouldBe "TromboneHcd.scala"
      log(LoggingKeys.LINE) shouldBe logMsgLineNumber
      log(LoggingKeys.CLASS) shouldBe "csw.services.logging.components.TromboneHcd"
      logMsgLineNumber += 1
    }
  }

  // DEOPSCSW-116: Make log messages identifiable with components
  // DEOPSCSW-119: Associate source with each log message
  // DEOPSCSW-121: Define structured tags for log messages
  test("inner class logs should contain source location in terms of file name, class name and line number") {
    new InnerSourceComponent().startLogging(logMsgMap)
    Thread.sleep(100)

    //   Verify that default level is TRACE in config
    LoggingState.componentsLoggingState(Constants.DEFAULT_KEY).componentLogLevel shouldBe LoggingLevels.TRACE

    var logMsgLineNumber = InnerSourceComponent.TRACE_LINE_NO

    logBuffer.foreach { log ⇒
      log(LoggingKeys.COMPONENT_NAME) shouldBe "InnerSourceComponent"
      log(LoggingKeys.FILE) shouldBe "InnerSourceComponent.scala"
      log(LoggingKeys.LINE) shouldBe logMsgLineNumber
      log(LoggingKeys.CLASS) shouldBe "csw.services.logging.components.InnerSourceComponent$InnerSource"
      logMsgLineNumber += 1
    }
  }

  // DEOPSCSW-116: Make log messages identifiable with components
  // DEOPSCSW-119: Associate source with each log message
  // DEOPSCSW-121: Define structured tags for log messages
  test("singleton object logs should contain source location in terms of file name, class name and line number") {
    SingletonComponent.startLogging(logMsgMap)
    Thread.sleep(100)

    //   Verify that default level is TRACE in config
    LoggingState.componentsLoggingState(Constants.DEFAULT_KEY).componentLogLevel shouldBe LoggingLevels.TRACE

    var logMsgLineNumber = SingletonComponent.TRACE_LINE_NO

    logBuffer.foreach { log ⇒
      log.contains(LoggingKeys.COMPONENT_NAME) shouldBe true
      log(LoggingKeys.COMPONENT_NAME) shouldBe "SingletonComponent"
      log(LoggingKeys.FILE) shouldBe "SingletonComponent.scala"
      log(LoggingKeys.LINE) shouldBe logMsgLineNumber
      log(LoggingKeys.CLASS) shouldBe "csw.services.logging.components.SingletonComponent"
      logMsgLineNumber += 1
    }
  }

  // DEOPSCSW-116: Make log messages identifiable with components
  // DEOPSCSW-119: Associate source with each log message
  // DEOPSCSW-121: Define structured tags for log messages
  test("object logs should allow user definable keys and values") {

    //  Use SingletonComponent for test, but works identically for all cases
    SingletonComponent.startLogging(logMsgMap, userMsgMap)
    Thread.sleep(100)

    //   Verify that default level is TRACE in config
    LoggingState.componentsLoggingState("default").componentLogLevel shouldBe LoggingLevels.TRACE

    var logMsgLineNumber = SingletonComponent.USER_TRACE_LINE_NO

    logBuffer.foreach { log ⇒
      //  Count the user messages for test at the end
      var userMsgCount = 0
      log.contains("@componentName") shouldBe true
      log("@componentName") shouldBe "SingletonComponent"
      log("file") shouldBe "SingletonComponent.scala"
      log("line") shouldBe logMsgLineNumber
      log("class") shouldBe "csw.services.logging.components.SingletonComponent"
      //  This verifies that the user keys are present and the value is correct
      //  Also make sure all user messages and values are present
      userMsgMap.foreach { m =>
        log(m._1) shouldBe userMsgMap(m._1)
        userMsgCount += 1
      }
      logMsgLineNumber += 1
      userMsgCount shouldBe userMsgMap.size
    }
  }

  // DEOPSCSW-126 : Configurability of logging characteristics for component / log instance
  test("should load default filter provided in configuration file and applied to normal logging messages") {

    //  TromboneHcd component is logging 6 messages
    //  As per the filter, hcd should log 5 message of all the levels except TRACE
    new TromboneHcd().startLogging(logMsgMap)
    Thread.sleep(200)

    //  Verify that level is DEBUG
    LoggingState.componentsLoggingState(TromboneHcd.COMPONENT_NAME).componentLogLevel shouldBe LoggingLevels.DEBUG

    //  TromboneHcd component is logging 6 messages each of unique level
    //  As per the filter, hcd should log 5 message of all level except TRACE
    logBuffer.size shouldBe 5

    val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json(LoggingKeys.COMPONENT_NAME).toString)
    val tromboneHcdLogs          = groupByComponentNamesLog("tromboneHcd")

    tromboneHcdLogs.size shouldBe 5

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    tromboneHcdLogs.toList.foreach { log ⇒
      val currentLogLevel = log(LoggingKeys.SEVERITY).toString.toLowerCase
      val currentLogMsg   = log(LoggingKeys.MESSAGE).toString
      Level(currentLogLevel) >= LoggingLevels.DEBUG shouldBe true
      currentLogMsg shouldBe logMsgMap(currentLogLevel)
    }
  }

  // DEOPSCSW-126 : Configurability of logging characteristics for component / log instance
  test("should apply default log level provided in configuration file for normal logging messages") {

    new TromboneAssembly().startLogging(logMsgMap)
    Thread.sleep(300)

    //   Verify that default level is TRACE in config
    LoggingState.componentsLoggingState(Constants.DEFAULT_KEY).componentLogLevel shouldBe LoggingLevels.TRACE

    //  TromboneAssembly component is logging 6 messages each of unique level
    //  As per the default loglevel = trace, assembly should log all 6 message
    logBuffer.size shouldBe 6

    val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json(LoggingKeys.COMPONENT_NAME).toString)
    val tromboneAssemblyLogs     = groupByComponentNamesLog("tromboneAssembly")

    tromboneAssemblyLogs.size shouldBe 6

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    tromboneAssemblyLogs.toList.foreach { log ⇒
      val currentLogLevel = log(LoggingKeys.SEVERITY).toString.toLowerCase
      val currentLogMsg   = log(LoggingKeys.MESSAGE).toString
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
      val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json(LoggingKeys.COMPONENT_NAME).toString)
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
        val currentLogLevel = log(LoggingKeys.SEVERITY).toString.toLowerCase
        val currentLogMsg   = log(LoggingKeys.MESSAGE).toString
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
    val compName = TromboneHcd.COMPONENT_NAME

    def filterLogsByComponentName(compName: String): Seq[JsonOps.JsonObject] = {
      val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json(LoggingKeys.COMPONENT_NAME).toString)
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
        val currentLogLevel = log(LoggingKeys.SEVERITY).toString.toLowerCase
        val currentLogMsg   = log(LoggingKeys.MESSAGE).toString
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
      log(LoggingKeys.CATEGORY) shouldBe true
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
      log(LoggingKeys.COMPONENT_NAME) shouldBe "tromboneHcd"
      log(LoggingKeys.SEVERITY) shouldBe ERROR.name
      log(LoggingKeys.CLASS) shouldBe tromboneHcdClassName
      log(LoggingKeys.MESSAGE) shouldBe computationResultMsg

      log.contains("trace") shouldBe true
      val traceBlock = log("trace").asInstanceOf[JsonObject]
      traceBlock.contains(LoggingKeys.MESSAGE) shouldBe true
      traceBlock.contains("stack") shouldBe true

      val traceMsgBlock = traceBlock(LoggingKeys.MESSAGE).asInstanceOf[JsonObject]
      traceMsgBlock.contains(LoggingKeys.EX) shouldBe true
      traceMsgBlock.contains(LoggingKeys.MESSAGE) shouldBe true
    }
  }

}
