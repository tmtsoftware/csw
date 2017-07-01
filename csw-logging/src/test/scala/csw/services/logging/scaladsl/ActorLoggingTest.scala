package csw.services.logging.scaladsl

import csw.services.logging.components.TromboneActor
import csw.services.logging.components.TromboneActor._
import csw.services.logging.internal.LoggingLevels
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.utils.LoggingTestSuite

class ActorLoggingTest extends LoggingTestSuite {
  private val tromboneActorRef =
    actorSystem.actorOf(TromboneActor.props(), name = "TromboneActor")

  def sendMessagesToActor() = {
    tromboneActorRef ! LogTrace
    tromboneActorRef ! LogDebug
    tromboneActorRef ! LogInfo
    tromboneActorRef ! LogWarn
    tromboneActorRef ! LogError
    tromboneActorRef ! LogFatal
    tromboneActorRef ! "Unknown"
    Thread.sleep(200)
  }

  // DEOPSCSW-116: Make log messages identifiable with components
  // DEOPSCSW-117: Provide unique name for each logging instance of components
  // DEOPSCSW-119: Associate source with each log message
  // DEOPSCSW-121: Define structured tags for log messages
  test("messages logged from actor should contain component name, file name, class name, line number and actor path") {

    sendMessagesToActor()

    // default log level for TromboneActor is ERROR in config
    var logMsgLineNumber = TromboneActor.ERROR_LINE_NO

    logBuffer.foreach { log ⇒
      log("@componentName") shouldBe "tromboneHcdActor"
      log("actor") shouldBe tromboneActorRef.path.toString
      log("file") shouldBe "TromboneActor.scala"
      log("line") shouldBe logMsgLineNumber
      log("class") shouldBe "csw.services.logging.components.TromboneActor"
      logMsgLineNumber += 1
    }
  }

  // DEOPSCSW-115: Format and control logging content
  // DEOPSCSW-121: Define structured tags for log messages
  test("message logged with custom Map properties should get logged") {
    tromboneActorRef ! "Unknown"
    Thread.sleep(200)

    val errorLevelLogMessages =
      logBuffer.groupBy(json ⇒ json("@severity"))("ERROR")
    errorLevelLogMessages.size shouldEqual 1
    val expectedMessage = Map("@msg" -> "Unknown message", "reason" -> "Unknown")
    errorLevelLogMessages.head("message") shouldBe expectedMessage
  }

  // DEOPSCSW-126 : Configurability of logging characteristics for component / log instance
  test("should load default filter provided in configuration file and applied to actor messages") {

    sendMessagesToActor()
    //  TromboneHcd component is logging 7 messages
    //  As per the filter, hcd should log 3 message of level ERROR and FATAL
    val groupByComponentNamesLog =
      logBuffer.groupBy(json ⇒ json("@componentName").toString)
    val tromboneHcdLogs = groupByComponentNamesLog("tromboneHcdActor")

    tromboneHcdLogs.size shouldBe 3

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    tromboneHcdLogs.toList.foreach { log ⇒
      log.contains("actor") shouldBe true
      val currentLogLevel = log("@severity").toString.toLowerCase
      Level(currentLogLevel) >= LoggingLevels.ERROR shouldBe true
    }

  }

}
