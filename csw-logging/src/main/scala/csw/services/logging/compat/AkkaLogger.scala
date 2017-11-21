package csw.services.logging.compat

import akka.actor.Actor
import akka.event.Logging._
import csw.services.logging.internal.{LogAkka, MessageHandler}

/**
 * Actors log by mixing the trait `akka.actor.ActorLogging`. This logger is used to allow akka logs to be sent to the common log.
 */
private[logging] class AkkaLogger extends Actor {
  import csw.services.logging.internal.LoggingLevels._

  private def log(level: Level, source: String, clazz: Class[_], msg: Any, time: Long, cause: Option[Throwable] = None): Unit = {
    val logAkka = LogAkka(time, level, source, clazz, msg, cause)
    MessageHandler.sendAkkaMsg(logAkka)
  }

  def receive: Receive = {
    case InitializeLogger(_) => sender ! LoggerInitialized
    case event @ Error(cause, logSource, logClass, message) =>
      val c = if (cause.toString.contains("NoCause$")) {
        None
      } else {
        Some(cause)
      }
      log(ERROR, logSource, logClass, message, event.timestamp, c)
    case event @ Warning(logSource, logClass, message) => log(WARN, logSource, logClass, message, event.timestamp)
    case event @ Info(logSource, logClass, message)    => log(INFO, logSource, logClass, message, event.timestamp)
    case event @ Debug(logSource, logClass, message)   => log(DEBUG, logSource, logClass, message, event.timestamp)
  }
}
