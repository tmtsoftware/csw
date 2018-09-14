package csw.logging.compat

import akka.actor.{Actor, ActorLogging, Props}
import csw.logging.commons.LoggingKeys
import csw.logging.internal.LoggingLevels
import csw.logging.internal.LoggingLevels.Level
import csw.logging.utils.LoggingTestSuite
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
      log.contains(LoggingKeys.COMPONENT_NAME) shouldBe false

      log.contains(LoggingKeys.SEVERITY) shouldBe true
      val currentLogLevel = log(LoggingKeys.SEVERITY).toString.toLowerCase
      Level(currentLogLevel) >= LoggingLevels.INFO shouldBe true

      log(LoggingKeys.MESSAGE) shouldBe currentLogLevel
      log(LoggingKeys.ACTOR) shouldBe actorRef.path.toString
      log(LoggingKeys.CLASS) shouldBe className
      log(LoggingKeys.KIND) shouldBe "akka"
    }

  }

}
