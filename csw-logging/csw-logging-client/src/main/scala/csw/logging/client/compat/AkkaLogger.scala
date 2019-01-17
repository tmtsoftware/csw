package csw.logging.client.compat

import akka.actor.Actor
import akka.event.Logging._
import csw.logging.client.internal.LogActorMessages.LogAkka
import csw.logging.client.internal.MessageHandler

/**
 * This actor is wired up as akka logger in `logging.conf`. The instance of this actor is created via reflection. When log
 * statement from akka code is executed, a message is sent to this actor. Then this actor will simply process the received message
 * and forward it to underlying logging code.
 */
private[logging] class AkkaLogger extends Actor {
  import csw.logging.api.models.LoggingLevels._

  def receive: Receive = {
    case InitializeLogger(_) => sender ! LoggerInitialized
    case event @ Error(cause, logSource, logClass, message) =>
      val c =
        if (cause.toString.contains("NoCause$")) None
        else Some(cause)
      log(ERROR, logSource, logClass, message, event.timestamp, c)
    case event @ Warning(logSource, logClass, message) => log(WARN, logSource, logClass, message, event.timestamp)
    case event @ Info(logSource, logClass, message)    => log(INFO, logSource, logClass, message, event.timestamp)
    case event @ Debug(logSource, logClass, message)   => log(DEBUG, logSource, logClass, message, event.timestamp)
  }

  private def log(level: Level, source: String, clazz: Class[_], msg: Any, time: Long, cause: Option[Throwable] = None): Unit =
    MessageHandler.sendAkkaMsg(LogAkka(time, level, source, clazz, msg, cause))
}
