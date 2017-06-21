package csw.services.logging.scaladsl

import akka.actor.Props
import csw.services.logging.internal.LoggingLevels
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.scaladsl.TromboneActor._
import csw.services.logging.utils.LoggingTestSuite

object TromboneLogger extends ComponentLogger("tromboneHcdActor")

object TromboneActor {
  def props() = Props(new TromboneActor())

  case object LogTrace
  case object LogDebug
  case object LogInfo
  case object LogWarn
  case object LogError
  case object LogFatal
}

class TromboneActor() extends TromboneLogger.Actor {

  def receive = {
    case LogTrace => log.trace("Level is trace")
    case LogDebug => log.debug("Level is debug")
    case LogInfo  => log.info("Level is info")
    case LogWarn  => log.warn("Level is warn")
    case LogError => log.error("Level is error")
    case LogFatal => log.fatal("Level is fatal")
    case x: Any =>
      log.error(Map("@errorMsg" -> "Unexpected actor message", "message" -> x.toString))
  }
}

class ActorLoggingTest extends LoggingTestSuite {
  private val tromboneActorRef =
    actorSystem.actorOf(TromboneActor.props(), name = "TromboneActor")

  override protected def afterEach(): Unit = logBuffer.clear()

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
  test("messages logged from actor should contain component name as well as actor path") {

    sendMessagesToActor()

    logBuffer.foreach { log ⇒
      log.contains("@componentName") shouldBe true
      log.contains("actor") shouldBe true
      log("@componentName") shouldBe "tromboneHcdActor"
      log("actor") shouldBe tromboneActorRef.path.toString
      log.contains("file") shouldBe true
      log.contains("line") shouldBe true
      log.contains("class") shouldBe true
    }
  }

  // DEOPSCSW-119: Make log messages identifiable with components
  test("messages logged from actor should contain source location in terms of file name, class name and line number") {

    sendMessagesToActor()

    logBuffer.foreach { log ⇒
      log("file") shouldBe "ActorLoggingTest.scala"
      log.contains("line") shouldBe true
      log("class") shouldBe "csw.services.logging.scaladsl.TromboneActor"
    }
  }

  test("message logged with custom Map properties should get logged") {
    tromboneActorRef ! "Unknown"
    Thread.sleep(200)

    val errorLevelLogMessages =
      logBuffer.groupBy(json ⇒ json("@severity"))("ERROR")
    errorLevelLogMessages.size shouldEqual 1
    val expectedMessage = Map("@errorMsg" -> "Unexpected actor message", "message" -> "Unknown")
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
