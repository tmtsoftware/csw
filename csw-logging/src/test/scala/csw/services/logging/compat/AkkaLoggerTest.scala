package csw.services.logging.compat

import akka.actor.{Actor, ActorLogging, Props}
import csw.services.logging.internal.LoggingLevels
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.utils.LoggingTestSuite
import org.scalatest.{FunSuiteLike, Matchers}

class MyActor extends Actor with ActorLogging {
  val exception = new RuntimeException("Exception occurred")

  def receive = {
    case "info"  ⇒ log.info("info")
    case "debug" ⇒ log.debug("debug")
    case "warn"  ⇒ log.warning("warn")
    case "error" ⇒ log.error(exception, "error")
  }
}

class AkkaLoggerTest extends LoggingTestSuite with FunSuiteLike with Matchers {

  test("logging framework should capture akka log messages and log it") {
    val actorRef  = actorSystem.actorOf(Props(new MyActor()), "my-actor")
    val className = classOf[MyActor].getName

    actorRef ! "info"
    actorRef ! "debug"
    actorRef ! "warn"
    actorRef ! "error"

    Thread.sleep(300)

    logBuffer.foreach { log ⇒
      log.contains("@componentName") shouldBe false

      log.contains("@severity") shouldBe true
      val currentLogLevel = log("@severity").toString.toLowerCase
      Level(currentLogLevel) >= LoggingLevels.INFO shouldBe true

      log("message") shouldBe currentLogLevel
      log("actor") shouldBe actorRef.path.toString
      log("class") shouldBe className
      log("kind") shouldBe "akka"
    }

  }

}
