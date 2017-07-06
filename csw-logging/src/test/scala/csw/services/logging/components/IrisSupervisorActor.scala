package csw.services.logging.components

import akka.actor.Props
import csw.services.logging.components.IrisSupervisorActor._
import csw.services.logging.scaladsl.{ComponentLogger, GenericLogger}

object IRISLogger extends ComponentLogger("IRIS")

class IrisSupervisorActor() extends IRISLogger.Actor {

  // Do not add any lines before this method
  // Tests are written to assert on this line numbers
  // In case any line needs to be added then update constants in companion object
  def receive = {
    case LogTrace => log.trace("Level is trace")
    case LogDebug => log.debug("Level is debug")
    case LogInfo  => log.info("Level is info")
    case LogWarn  => log.warn("Level is warn")
    case LogError => log.error("Level is error")
    case LogFatal => log.fatal("Level is fatal")
    case x: Any   => log.error(Map("@msg" -> "Unknown message", "reason" -> x.toString))
  }
}

object IrisSupervisorActor {

  val TRACE_LINE_NO = 15
  val DEBUG_LINE_NO = TRACE_LINE_NO + 1
  val INFO_LINE_NO  = TRACE_LINE_NO + 2
  val WARN_LINE_NO  = TRACE_LINE_NO + 3
  val ERROR_LINE_NO = TRACE_LINE_NO + 4
  val FATAL_LINE_NO = TRACE_LINE_NO + 5
  val ANY_LINE_NO   = TRACE_LINE_NO + 6

  val NAME       = "IRIS"
  val CLASS_NAME = "csw.services.logging.components.IrisSupervisorActor"
  val FILE_NAME  = "IrisSupervisorActor.scala"

  case object LogTrace
  case object LogDebug
  case object LogInfo
  case object LogWarn
  case object LogError
  case object LogFatal

  def props() = Props(new IrisSupervisorActor())
}

class IrisTLA extends IRISLogger.Simple {
  def startLogging(logs: Map[String, String]): Unit = {
    log.trace(logs("trace"))
    log.debug(logs("debug"))
    log.info(logs("info"))
    log.warn(logs("warn"))
    log.error(logs("error"))
    log.fatal(logs("fatal"))
  }
}

class IrisUtil extends GenericLogger.Simple {
  def startLogging(logs: Map[String, String]): Unit = {
    log.trace(logs("trace"))
    log.debug(logs("debug"))
    log.info(logs("info"))
    log.warn(logs("warn"))
    log.error(logs("error"))
    log.fatal(logs("fatal"))
  }
}

class IrisActorUtil extends GenericLogger.Actor {
  def receive = {
    case LogTrace => log.trace("Level is trace")
    case LogDebug => log.debug("Level is debug")
    case LogInfo  => log.info("Level is info")
    case LogWarn  => log.warn("Level is warn")
    case LogError => log.error("Level is error")
    case LogFatal => log.fatal("Level is fatal")
    case x: Any   => log.error(Map("@msg" -> "Unknown message", "reason" -> x.toString))
  }
}

object IrisActorUtil {
  def props = Props(new IrisActorUtil)
}
