package csw.services.logging.scaladsl

import akka.actor.{ActorSystem, Props}
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
  private val system = ActorSystem("test")

  override protected def afterEach(): Unit = logBuffer.clear()

  override protected def afterAll(): Unit = {
    Await.result(loggingSystem.stop, 10.seconds)
    Await.result(system.terminate(), 10.seconds)
  }

  // DEOPSCSW-116: Make log messages identifiable with components
  test("messages logged from actor should contain component name as well as actor path") {
    val tromboneActorRef = system.actorOf(TromboneActor.props(), name = "TromboneActor")

    tromboneActorRef ! LogTrace
    tromboneActorRef ! LogDebug
    tromboneActorRef ! LogInfo
    tromboneActorRef ! LogWarn
    tromboneActorRef ! LogError
    tromboneActorRef ! LogFatal
    tromboneActorRef ! "Unknown"

    Thread.sleep(100)

    logBuffer.foreach { log ⇒
      log.contains("@componentName") shouldBe true
      log.contains("actor") shouldBe true
      log("@componentName") shouldBe "tromboneHcdActor"
      log("actor") shouldBe tromboneActorRef.path.toString
    }
  }
}
