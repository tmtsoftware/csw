package csw.services.logging.compat

import akka.actor.{Actor, ActorLogging, Props}
import csw.services.logging.commons.Keys
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
      log.contains(Keys.COMPONENT_NAME) shouldBe false

      log.contains(Keys.SEVERITY) shouldBe true
      val currentLogLevel = log(Keys.SEVERITY).toString.toLowerCase
      Level(currentLogLevel) >= LoggingLevels.INFO shouldBe true

      log(Keys.MESSAGE) shouldBe currentLogLevel
      log(Keys.ACTOR) shouldBe actorRef.path.toString
      log(Keys.CLASS) shouldBe className
      log(Keys.KIND) shouldBe "akka"
    }

  }

}
