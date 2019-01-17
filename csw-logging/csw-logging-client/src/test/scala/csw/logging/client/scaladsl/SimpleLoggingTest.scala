package csw.logging.client.scaladsl

import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import csw.logging.client.commons.{Constants, LoggingKeys, TMTDateTimeFormatter}
import csw.logging.client.components.{InnerSourceComponent, SingletonComponent, TromboneAssembly, TromboneHcd}
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.api.models.LoggingLevels._
import csw.logging.api.models.LoggingLevels
import csw.logging.client.internal.LoggingState
import csw.logging.client.utils.LoggingTestSuite
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.libs.json.JsObject

class SimpleLoggingTest extends LoggingTestSuite {

  // DEOPSCSW-116: Make log messages identifiable with components
  // DEOPSCSW-118: Provide UTC time for each log message
  // DEOPSCSW-119: Associate source with each log message
  // DEOPSCSW-121: Define structured tags for log messages
  test("logs should contain component name and source location in terms of file name, class name and line number") {
    val expectedDateTime = ZonedDateTime.now(ZoneId.from(ZoneOffset.UTC))
    new TromboneHcd().startLogging(logMsgMap)
    Thread.sleep(300)

    // Verify log level for tromboneHcd is at debug level in config
    LoggingState.componentsLoggingState(TromboneHcd.COMPONENT_NAME).componentLogLevel shouldBe LoggingLevels.DEBUG

    var logMsgLineNumber = TromboneHcd.DEBUG_LINE_NO

    logBuffer.size shouldBe 5
    logBuffer.foreach { log ⇒
      // This assert's that, ISO_INSTANT parser should not throw exception while parsing timestamp from log message
      // If timestamp is in other than UTC(ISO_FORMAT) format, DateTimeFormatter.ISO_INSTANT will throw DateTimeParseException
      // Ex.  2017-07-19T13:23:55.358+03:00 :=> DateTimeFormatter.ISO_INSTANT.parse will throw exception
      //      2017-07-19T01:23:55.360+05:30 :=> DateTimeFormatter.ISO_INSTANT.parse will throw exception
      //      2017-07-19T01:23:55.360Z      :=> DateTimeFormatter.ISO_INSTANT.parse will not throw exception
      noException shouldBe thrownBy(DateTimeFormatter.ISO_INSTANT.parse(log.getString(LoggingKeys.TIMESTAMP)))

      val actualDateTime = TMTDateTimeFormatter.parse(log.getString(LoggingKeys.TIMESTAMP))
      ChronoUnit.MILLIS.between(expectedDateTime, actualDateTime) <= 50 shouldBe true

      log.getString(LoggingKeys.COMPONENT_NAME) shouldBe "tromboneHcd"
      log.getString(LoggingKeys.FILE) shouldBe "TromboneHcd.scala"
      log(LoggingKeys.LINE).as[Int] shouldBe logMsgLineNumber
      log.getString(LoggingKeys.CLASS) shouldBe "csw.logging.client.components.TromboneHcd"
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

    logBuffer.size shouldBe 6
    logBuffer.foreach { log ⇒
      log.getString(LoggingKeys.COMPONENT_NAME) shouldBe "InnerSourceComponent"
      log.getString(LoggingKeys.FILE) shouldBe "InnerSourceComponent.scala"
      log(LoggingKeys.LINE).as[Int] shouldBe logMsgLineNumber
      log.getString(LoggingKeys.CLASS) shouldBe "csw.logging.client.components.InnerSourceComponent$InnerSource"
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

    logBuffer.size shouldBe 6
    logBuffer.foreach { log ⇒
      log.contains(LoggingKeys.COMPONENT_NAME) shouldBe true
      log.getString(LoggingKeys.COMPONENT_NAME) shouldBe "SingletonComponent"
      log.getString(LoggingKeys.FILE) shouldBe "SingletonComponent.scala"
      log(LoggingKeys.LINE).as[Int] shouldBe logMsgLineNumber
      log.getString(LoggingKeys.CLASS) shouldBe "csw.logging.client.components.SingletonComponent"
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

    logBuffer.size shouldBe 6
    logBuffer.foreach { log ⇒
      //  Count the user messages for test at the end
      var userMsgCount = 0
      log.contains("@componentName") shouldBe true
      log.getString("@componentName") shouldBe "SingletonComponent"
      log.getString("file") shouldBe "SingletonComponent.scala"
      log("line").as[Int] shouldBe logMsgLineNumber
      log.getString("class") shouldBe "csw.logging.client.components.SingletonComponent"
      //  This verifies that the user keys are present and the value is correct
      //  Also make sure all user messages and values are present
      userMsgMap.foreach { m =>
        log.getString(m._1) shouldBe userMsgMap(m._1)
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

    val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json.getString(LoggingKeys.COMPONENT_NAME))
    val tromboneHcdLogs          = groupByComponentNamesLog("tromboneHcd")

    tromboneHcdLogs.size shouldBe 5

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    tromboneHcdLogs.toList.foreach { log ⇒
      val currentLogLevel = log.getString(LoggingKeys.SEVERITY).toLowerCase
      val currentLogMsg   = log.getString(LoggingKeys.MESSAGE)
      Level(currentLogLevel) >= LoggingLevels.DEBUG shouldBe true
      currentLogMsg shouldBe logMsgMap(currentLogLevel)
    }
  }

  // DEOPSCSW-126 : Configurability of logging characteristics for component / log instance
  test("should apply default log level provided in configuration file for normal logging messages") {

    new TromboneAssembly(new LoggerFactory("tromboneAssembly")).startLogging(logMsgMap)
    Thread.sleep(300)

    //   Verify that default level is TRACE in config
    LoggingState.componentsLoggingState(Constants.DEFAULT_KEY).componentLogLevel shouldBe LoggingLevels.TRACE

    //  TromboneAssembly component is logging 6 messages each of unique level
    //  As per the default loglevel = trace, assembly should log all 6 message
    logBuffer.size shouldBe 6

    val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json.getString(LoggingKeys.COMPONENT_NAME))
    val tromboneAssemblyLogs     = groupByComponentNamesLog("tromboneAssembly")

    tromboneAssemblyLogs.size shouldBe 6

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    tromboneAssemblyLogs.toList.foreach { log ⇒
      val currentLogLevel = log.getString(LoggingKeys.SEVERITY).toLowerCase
      val currentLogMsg   = log.getString(LoggingKeys.MESSAGE)
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

    def filterLogsByComponentName(compName: String): Seq[JsObject] = {
      val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json.getString(LoggingKeys.COMPONENT_NAME))
      groupByComponentNamesLog(compName)
    }

    val defaultLogLevel = loggingSystem.getDefaultLogLevel

    forAll(testData) { (logLevel: Level, logCount: Int) =>
      loggingSystem.setComponentLogLevel(compName, logLevel)

      // confirm default log level is not changing when we change the component log level
      loggingSystem.getDefaultLogLevel shouldBe defaultLogLevel

      new TromboneAssembly(new LoggerFactory("tromboneAssembly")).startLogging(logMsgMap)
      Thread.sleep(200)

      val tromboneAssemblyLogs = filterLogsByComponentName(compName)
      tromboneAssemblyLogs.size shouldBe logCount

      tromboneAssemblyLogs.toList.foreach { log ⇒
        val currentLogLevel = log.getString(LoggingKeys.SEVERITY).toLowerCase
        val currentLogMsg   = log.getString(LoggingKeys.MESSAGE)
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

    def filterLogsByComponentName(compName: String): Seq[JsObject] = {
      val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json.getString(LoggingKeys.COMPONENT_NAME))
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
        val currentLogLevel = log.getString(LoggingKeys.SEVERITY).toLowerCase
        val currentLogMsg   = log.getString(LoggingKeys.MESSAGE)
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
      log.getString(LoggingKeys.CATEGORY) shouldBe true
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
     *   "class":"csw.logging.client.scaladsl.TromboneHcd",
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
      log.getString(LoggingKeys.COMPONENT_NAME) shouldBe "tromboneHcd"
      log.getString(LoggingKeys.SEVERITY) shouldBe ERROR.name
      log.getString(LoggingKeys.CLASS) shouldBe tromboneHcdClassName
      log.getString(LoggingKeys.MESSAGE) shouldBe computationResultMsg
      // DEOPSCSW-325: Include exception stack trace in stdout log message for exceptions
      log.contains(LoggingKeys.PLAINSTACK) shouldBe true

      log.contains("trace") shouldBe true
      val traceBlock = log("trace").as[JsObject]
      traceBlock.contains(LoggingKeys.MESSAGE) shouldBe true
      traceBlock.contains("stack") shouldBe true

      val traceMsgBlock = traceBlock(LoggingKeys.MESSAGE).as[JsObject]
      traceMsgBlock.contains(LoggingKeys.EX) shouldBe true
      traceMsgBlock.contains(LoggingKeys.MESSAGE) shouldBe true
    }

    // DEOPSCSW-325: Include exception stack trace in stdout log message for exceptions
    val plainstack = logBuffer.head.getString(LoggingKeys.PLAINSTACK)
    plainstack.startsWith("java.lang.ArithmeticException: / by zero") shouldBe true
  }

}
