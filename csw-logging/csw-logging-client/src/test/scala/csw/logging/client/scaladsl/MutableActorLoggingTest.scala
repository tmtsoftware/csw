package csw.logging.client.scaladsl

import akka.actor.typed.scaladsl.Behaviors
import csw.logging.models.Level.ERROR
import csw.logging.api.scaladsl.Logger
import csw.logging.client.LogCommand
import csw.logging.client.LogCommand._
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.utils.LoggingTestSuite
import csw.logging.models.Level
import csw.prefix.models.Prefix

object TromboneMutableActor {
  def behavior(loggerFactory: LoggerFactory): Behaviors.Receive[LogCommand] =
    Behaviors.receive { (ctx, msg) =>
      val log: Logger = loggerFactory.getLogger(ctx.self)

      msg match {
        case LogTrace => log.trace("Level is trace")
        case LogDebug => log.debug("Level is debug")
        case LogInfo  => log.info("Level is info")
        case LogWarn  => log.warn("Level is warn")
        case LogError => log.error("Level is error")
        case LogFatal => log.fatal("Level is fatal")
        case Unknown  => log.error("Unexpected actor message", Map("message" -> Unknown))
      }
      Behaviors.same
    }
}

// DEOPSCSW-280 SPIKE: Introduce Akkatyped in logging
class MutableActorLoggingTest extends LoggingTestSuite {

  private val tromboneActorRef =
    actorSystem.spawn(
      TromboneMutableActor.behavior(new LoggerFactory(Prefix("csw.tromboneMutableHcdActor"))),
      "csw.TromboneMutableActor"
    )

  def sendMessagesToActor(): Unit = {
    tromboneActorRef ! LogTrace
    tromboneActorRef ! LogDebug
    tromboneActorRef ! LogInfo
    tromboneActorRef ! LogWarn
    tromboneActorRef ! LogError
    tromboneActorRef ! LogFatal
    tromboneActorRef ! Unknown
    Thread.sleep(300)
  }

  // DEOPSCSW-116: Make log messages identifiable with components
  // DEOPSCSW-117: Provide unique name for each logging instance of components
  // DEOPSCSW-119: Associate source with each log message
  // DEOPSCSW-121: Define structured tags for log messages
  // CSW-86: Subsystem should be case-insensitive
  test(
    "messages logged from actor should contain component name, file name, class name, line number and actor path | DEOPSCSW-280, DEOPSCSW-121, DEOPSCSW-117, DEOPSCSW-116, DEOPSCSW-119"
  ) {

    sendMessagesToActor()

    logBuffer.foreach { log =>
      log.contains("@componentName") shouldBe true
      log.contains("actor") shouldBe true
      log.getString("@componentName") shouldBe "tromboneMutableHcdActor"
      log.getString("@subsystem") shouldBe "CSW"
      log.getString("actor") shouldBe tromboneActorRef.path.toString
      log.getString("file") shouldBe "MutableActorLoggingTest.scala"
      log.contains("line") shouldBe true
      log.getString("class") shouldBe "csw.logging.client.scaladsl.TromboneMutableActor"
    }
  }

  // DEOPSCSW-126 : Configurability of logging characteristics for component / log instance
  test("should load default filter provided in configuration file and applied to actor messages | DEOPSCSW-280, DEOPSCSW-126") {

    sendMessagesToActor()
    //  TromboneHcd component is logging 7 messages
    //  As per the filter, hcd should log 3 message of level ERROR and FATAL
    val groupByComponentNamesLog =
      logBuffer.groupBy(json => json.getString("@componentName"))
    val tromboneHcdLogs = groupByComponentNamesLog("tromboneMutableHcdActor")

    tromboneHcdLogs.size shouldBe 3

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    tromboneHcdLogs.toList.foreach { log =>
      log.contains("actor") shouldBe true
      val currentLogLevel = log.getString("@severity").toLowerCase
      Level(currentLogLevel) >= ERROR shouldBe true
    }
  }
}
