package csw.services.logging

import akka.actor.Actor
import akka.event.Logging._
import LogActor.AkkaMessage

private[logging] class AkkaLogger extends Actor {
  import LoggingLevels._

  private def log(level: Level,
                  source: String,
                  clazz: Class[_],
                  msg: Any,
                  time: Long,
                  cause: Option[Throwable] = None) {
    val m = AkkaMessage(time, level, source, clazz, msg, cause)
    LoggingState.akkaMsg(m)
  }

  def receive: PartialFunction[Any, Unit] = {
    case InitializeLogger(_) => sender ! LoggerInitialized
    case event @ Error(cause, logSource, logClass, message) =>
      val c = if (cause.toString().contains("NoCause$")) {
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
