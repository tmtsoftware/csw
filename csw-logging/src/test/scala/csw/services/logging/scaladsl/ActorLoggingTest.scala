package csw.services.logging.scaladsl

import akka.actor.Props
import csw.services.logging.internal.LoggingLevels
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.scaladsl.TromboneActor._
import csw.services.logging.utils.LoggingTestSuite

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

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
    case x: Any   => log.error(Map("@msg" -> "Unexpected actor message", "message" -> x.toString))
  }
}

class ActorLoggingTest extends LoggingTestSuite {
  private val tromboneActorRef = actorSystem.actorOf(TromboneActor.props(), name = "TromboneActor")

  override protected def afterEach(): Unit = logBuffer.clear()

  override protected def afterAll(): Unit = {
    Await.result(loggingSystem.stop, 10.seconds)
    Await.result(actorSystem.terminate(), 10.seconds)
  }

  def sendMessagesToActor() = {
    tromboneActorRef ! LogTrace
    tromboneActorRef ! LogDebug
    tromboneActorRef ! LogInfo
    tromboneActorRef ! LogWarn
    tromboneActorRef ! LogError
    tromboneActorRef ! LogFatal
    tromboneActorRef ! "Unknown"
    Thread.sleep(100)
  }

  // DEOPSCSW-116: Make log messages identifiable with components
  test("messages logged from actor should contain component name as well as actor path") {

    sendMessagesToActor()

    logBuffer.foreach { log ⇒
      log.contains("@componentName") shouldBe true
      log.contains("actor") shouldBe true
      log("@componentName") shouldBe "tromboneHcdActor"
      log("actor") shouldBe tromboneActorRef.path.toString
    }
  }

  // DEOPSCSW-126 : Configurability of logging characteristics for component / log instance
  test("should load default filter provided in configuration file and applied to actor messages") {

    sendMessagesToActor()
    //  TromboneHcd component is logging 7 messages
    //  As per the filter, hcd should log 3 message of level ERROR and FATAL
    val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json("@componentName").toString)
    val tromboneHcdLogs          = groupByComponentNamesLog.get("tromboneHcdActor").get

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
