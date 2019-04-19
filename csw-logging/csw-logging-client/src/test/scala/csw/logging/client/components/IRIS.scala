package csw.logging.client.components

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.logging.api.scaladsl._
import csw.logging.client.components.IRIS._
import csw.logging.client.scaladsl.{GenericLoggerFactory, LoggerFactory}

// DEOPSCSW-316: Improve Logger accessibility for component developers
object IRISLibraryLogger extends LoggerFactory(IRIS.COMPONENT_NAME)

class IRIS(logger: LoggerFactory) {

  def behavior: Behavior[IRISLogMessages] = Behaviors.setup[IRISLogMessages] { ctx =>
    // DEOPSCSW-316: Improve Logger accessibility for component developers
    val log: Logger = logger.getLogger(ctx)
    Behaviors.receiveMessage[IRISLogMessages] { msg =>
      // Do not add any lines before this method
      // Tests are written to assert on this line numbers
      // In case any line needs to be added then update constants in companion object
      msg match {
        case LogTrace           => log.trace(irisLogs("trace"))
        case LogDebug           => log.debug(irisLogs("debug"))
        case LogInfo            => log.info(irisLogs("info"))
        case LogWarn            => log.warn(irisLogs("warn"))
        case LogError           => log.error(irisLogs("error"))
        case LogFatal           => log.fatal(irisLogs("fatal"))
        case LogErrorWithMap(x) => log.error("Logging error with map", Map("reason" -> x, "actorRef" → ctx.self.toString))
      }
      Behaviors.same
    }
  }
}

object IRIS {

  val TRACE_LINE_NO         = 22
  val DEBUG_LINE_NO         = TRACE_LINE_NO + 1
  val INFO_LINE_NO          = TRACE_LINE_NO + 2
  val WARN_LINE_NO          = TRACE_LINE_NO + 3
  val ERROR_LINE_NO         = TRACE_LINE_NO + 4
  val FATAL_LINE_NO         = TRACE_LINE_NO + 5
  val LOG_ERROR_WITH_MAP_NO = TRACE_LINE_NO + 6

  val COMPONENT_NAME = "IRIS"
  val CLASS_NAME     = "csw.logging.client.components.IRIS"
  val FILE_NAME      = "IRIS.scala"

  sealed trait IRISLogMessages

  case object LogTrace extends IRISLogMessages
  case object LogDebug extends IRISLogMessages
  case object LogInfo  extends IRISLogMessages
  case object LogWarn  extends IRISLogMessages
  case object LogError extends IRISLogMessages
  case object LogFatal extends IRISLogMessages

  case class LogErrorWithMap(x: String) extends IRISLogMessages

  def behavior(componentName: String): Behavior[IRISLogMessages] = new IRIS(new LoggerFactory(componentName)).behavior

  val irisLogs = Map(
    "trace" → "iris: trace",
    "debug" → "iris: debug",
    "info"  → "iris: info",
    "warn"  → "iris: warn",
    "error" → "iris: error",
    "fatal" → "iris: fatal"
  )
}

class IrisTLA {
  import IRIS._

  // DEOPSCSW-316: Improve Logger accessibility for component developers
  val log: Logger = IRISLibraryLogger.getLogger

  def startLogging(): Unit = {
    log.trace(irisLogs("trace"))
    log.debug(irisLogs("debug"))
    log.info(irisLogs("info"))
    log.warn(irisLogs("warn"))
    log.error(irisLogs("error"))
    log.fatal(irisLogs("fatal"))
  }
}

class IrisUtil {

  // DEOPSCSW-316: Improve Logger accessibility for component developers
  val log: Logger = GenericLoggerFactory.getLogger

  def startLogging(logs: Map[String, String]): Unit = {
    log.trace(irisLogs("trace"))
    log.debug(irisLogs("debug"))
    log.info(irisLogs("info"))
    log.warn(irisLogs("warn"))
    log.error(irisLogs("error"))
    log.fatal(irisLogs("fatal"))
  }
}

object IrisActorUtil {

  def behavior: Behavior[IRISLogMessages] = Behaviors.setup[IRISLogMessages] { ctx =>
    // DEOPSCSW-316: Improve Logger accessibility for component developers
    val log: Logger = GenericLoggerFactory.getLogger(ctx)

    Behaviors.receiveMessage[IRISLogMessages] { msg =>
      msg match {
        case LogTrace           => log.trace(irisLogs("trace"))
        case LogDebug           => log.debug(irisLogs("debug"))
        case LogInfo            => log.info(irisLogs("info"))
        case LogWarn            => log.warn(irisLogs("warn"))
        case LogError           => log.error(irisLogs("error"))
        case LogFatal           => log.fatal(irisLogs("fatal"))
        case LogErrorWithMap(x) => log.error("Logging error with map", Map("reason" -> x, "actorRef" → ctx.self.toString))
      }
      Behaviors.same
    }
  }
}
