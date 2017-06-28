package csw.services.logging.scaladsl

import csw.services.logging.RichMsg
import csw.services.logging.internal.LoggingLevels._
import csw.services.logging.internal.LoggingState._
import csw.services.logging.internal.{Log, LogAltMessage, MessageHandler}
import csw.services.logging.macros.{SourceFactory, SourceLocation}
import org.jboss.netty.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

class LoggerImpl private[logging] (componentName: Option[String], actorName: Option[String]) extends Logger {

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  private def all(level: Level, id: AnyId, msg: => Any, ex: Throwable, sourceLocation: SourceLocation): Unit = {
    val t = System.currentTimeMillis()
    MessageHandler.sendMsg(Log(componentName, level, id, t, actorName, msg, sourceLocation, ex))
  }

  private def has(id: AnyId, level: Level): Boolean =
    id match {
      case id1: RequestId =>
        id1.level match {
          case Some(level1) => level.pos <= level1.pos
          case None         => false
        }
      case noId => false
    }

  def trace(msg: => RichMsg, ex: Throwable, id: AnyId)(implicit factory: SourceFactory): Unit =
    if (doTrace || has(id, TRACE)) all(TRACE, id, msg, ex, factory.get())

  def debug(msg: => RichMsg, ex: Throwable, id: AnyId)(implicit factory: SourceFactory): Unit =
    if (doDebug || has(id, DEBUG)) all(DEBUG, id, msg, ex, factory.get())

  override def info(msg: => RichMsg, ex: Throwable, id: AnyId)(implicit factory: SourceFactory): Unit =
    if (doInfo || has(id, INFO)) all(INFO, id, msg, ex, factory.get())

  override def warn(msg: => RichMsg, ex: Throwable, id: AnyId)(implicit factory: SourceFactory): Unit =
    if (doWarn || has(id, WARN)) all(WARN, id, msg, ex, factory.get())

  override def error(msg: => RichMsg, ex: Throwable, id: AnyId)(implicit factory: SourceFactory): Unit =
    if (doError || has(id, ERROR)) all(ERROR, id, msg, ex, factory.get())

  override def fatal(msg: => RichMsg, ex: Throwable, id: AnyId)(implicit factory: SourceFactory): Unit =
    all(FATAL, id, msg, ex, factory.get())

  private[logging] override def alternative(category: String,
                                            m: Map[String, RichMsg],
                                            ex: Throwable,
                                            id: AnyId,
                                            time: Long): Unit =
    MessageHandler.sendMsg(LogAltMessage(category, time, m ++ Map("@category" -> category), id, ex))
}
