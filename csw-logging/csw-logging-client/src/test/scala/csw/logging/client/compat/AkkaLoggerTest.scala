package csw.logging.client.compat

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.logging.api.models.LoggingLevels
import csw.logging.api.models.LoggingLevels.Level
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.logging.client.commons.LoggingKeys
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.utils.LoggingTestSuite
import org.scalatest.{FunSuiteLike, Matchers}

object MyActor {
  val exception = new RuntimeException("Exception occurred")

  def behavior: Behavior[String] = Behaviors.setup[String] { ctx =>
    ctx.setLoggerClass(this.getClass)
    Behaviors.receiveMessage[String] { msg =>
      msg match {
        case "info"  ⇒ ctx.log.info("info")
        case "debug" ⇒ ctx.log.debug("debug")
        case "warn"  ⇒ ctx.log.warning("warn")
        case "error" ⇒ ctx.log.error(exception, "error")
      }
      Behaviors.same
    }
  }

}

class AkkaLoggerTest extends LoggingTestSuite with FunSuiteLike with Matchers {

  test("logging framework should capture akka log messages and log it") {
    val actorRef  = actorSystem.userActorOf(MyActor.behavior, "my-actor")
    val className = MyActor.getClass.getName

    actorRef ! "info"
    actorRef ! "debug"
    actorRef ! "warn"
    actorRef ! "error"

    Thread.sleep(300)

    logBuffer.foreach { log ⇒
      log.contains(LoggingKeys.COMPONENT_NAME) shouldBe false

      log.contains(LoggingKeys.SEVERITY) shouldBe true
      val currentLogLevel = log.getString(LoggingKeys.SEVERITY).toLowerCase
      Level(currentLogLevel) >= LoggingLevels.INFO shouldBe true

      log.getString(LoggingKeys.MESSAGE) shouldBe currentLogLevel
      log.getString(LoggingKeys.ACTOR) shouldBe actorRef.path.toString
      log.getString(LoggingKeys.CLASS) shouldBe className
      log.getString(LoggingKeys.KIND) shouldBe "akka"
    }

  }

}
