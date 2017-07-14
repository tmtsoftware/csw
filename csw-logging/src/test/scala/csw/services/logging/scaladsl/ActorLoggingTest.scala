package csw.services.logging.scaladsl

import csw.services.logging.commons.LoggingKeys
import csw.services.logging.components.IRIS
import csw.services.logging.components.IRIS._
import csw.services.logging.internal.LoggingLevels
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.utils.LoggingTestSuite

class ActorLoggingTest extends LoggingTestSuite {
  private val irisActorRef =
    actorSystem.actorOf(IRIS.props(), name = "IRIS-Supervisor-Actor")

  def sendMessagesToActor() = {
    irisActorRef ! LogTrace
    irisActorRef ! LogDebug
    irisActorRef ! LogInfo
    irisActorRef ! LogWarn
    irisActorRef ! LogError
    irisActorRef ! LogFatal
    irisActorRef ! "Unknown"
    Thread.sleep(200)
  }

  // DEOPSCSW-116: Make log messages identifiable with components
  // DEOPSCSW-117: Provide unique name for each logging instance of components
  // DEOPSCSW-119: Associate source with each log message
  // DEOPSCSW-121: Define structured tags for log messages
  test("messages logged from actor should contain component name, file name, class name, line number and actor path") {

    sendMessagesToActor()

    // default log level for IrisSupervisorActor is ERROR in config
    var logMsgLineNumber = IRIS.ERROR_LINE_NO

    logBuffer.foreach { log ⇒
      log(LoggingKeys.COMPONENT_NAME) shouldBe IRIS.COMPONENT_NAME
      log(LoggingKeys.ACTOR) shouldBe irisActorRef.path.toString
      log(LoggingKeys.FILE) shouldBe IRIS.FILE_NAME
      log(LoggingKeys.LINE) shouldBe logMsgLineNumber
      log(LoggingKeys.CLASS).toString shouldBe IRIS.CLASS_NAME
      logMsgLineNumber += 1
    }
  }

  // DEOPSCSW-115: Format and control logging content
  // DEOPSCSW-121: Define structured tags for log messages
  test("message logged with custom Map properties should get logged") {
    irisActorRef ! "Unknown"
    Thread.sleep(200)

    val errorLevelLogMessages =
      logBuffer.groupBy(json ⇒ json(LoggingKeys.SEVERITY))("ERROR")
    errorLevelLogMessages.size shouldEqual 1

    val expectedMessage  = "Unknown message received"
    val expectedReason   = "Unknown"
    val expectedActorRef = irisActorRef.toString
    errorLevelLogMessages.head("message") shouldBe expectedMessage
    errorLevelLogMessages.head("reason") shouldBe expectedReason
    errorLevelLogMessages.head("actorRef") shouldBe expectedActorRef
  }

  // DEOPSCSW-126 : Configurability of logging characteristics for component / log instance
  test("should load default filter provided in configuration file and applied to actor messages") {

    sendMessagesToActor()
    //  IrisSupervisorActor is logging 7 messages
    //  As per the filter, hcd should log 3 message of level ERROR and FATAL
    val groupByComponentNamesLog =
      logBuffer.groupBy(json ⇒ json(LoggingKeys.COMPONENT_NAME).toString)
    val irisLogs = groupByComponentNamesLog(IRIS.COMPONENT_NAME)

    irisLogs.size shouldBe 3

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    irisLogs.toList.foreach { log ⇒
      log.contains(LoggingKeys.ACTOR) shouldBe true
      val currentLogLevel = log(LoggingKeys.SEVERITY).toString.toLowerCase
      Level(currentLogLevel) >= LoggingLevels.ERROR shouldBe true
    }

  }
}
