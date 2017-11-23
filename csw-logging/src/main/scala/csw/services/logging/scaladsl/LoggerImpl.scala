package csw.services.logging.scaladsl

import java.time.Instant

import csw.services.logging.RichMsg
import csw.services.logging.commons.{Constants, LoggingKeys}
import csw.services.logging.internal.LoggingLevels._
import csw.services.logging.internal.LoggingState._
import csw.services.logging.internal.{Log, LogAltMessage, MessageHandler}
import csw.services.logging.macros.{SourceFactory, SourceLocation}
import csw.services.logging.models.ComponentLoggingState
import org.jboss.netty.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

class LoggerImpl private[logging] (maybeComponentName: Option[String], actorName: ⇒ Option[String]) extends Logger {

  // this is to apply default log level for non-component classes like some common file utility classes
  private[this] val componentName: String = maybeComponentName.getOrElse(Constants.DEFAULT_KEY)

  // default log level will be applied if component specific log level is not provided in logging configuration inside component-log-levels block
  private[this] def componentLoggingState: ComponentLoggingState =
    componentsLoggingState.getOrElse(componentName, componentsLoggingState(Constants.DEFAULT_KEY))

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  private def all(level: Level,
                  id: AnyId,
                  msg: => String,
                  map: ⇒ Map[String, Any],
                  ex: Throwable,
                  sourceLocation: SourceLocation): Unit = {
    val time = Instant.now().toEpochMilli
    MessageHandler.sendMsg(Log(maybeComponentName, level, id, time, actorName, msg, map, sourceLocation, ex))
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

  def trace(msg: ⇒ String, map: ⇒ Map[String, Any], ex: Throwable, id: AnyId)(implicit factory: SourceFactory): Unit = {
    if (componentLoggingState.doTrace || has(id, TRACE)) all(TRACE, id, msg, map, ex, factory.get())
  }

  def debug(msg: ⇒ String, map: ⇒ Map[String, Any], ex: Throwable, id: AnyId)(implicit factory: SourceFactory): Unit =
    if (componentLoggingState.doDebug || has(id, DEBUG)) all(DEBUG, id, msg, map, ex, factory.get())

  override def info(msg: ⇒ String, map: ⇒ Map[String, Any], ex: Throwable, id: AnyId)(
      implicit factory: SourceFactory
  ): Unit =
    if (componentLoggingState.doInfo || has(id, INFO)) all(INFO, id, msg, map, ex, factory.get())

  override def warn(msg: ⇒ String, map: ⇒ Map[String, Any], ex: Throwable, id: AnyId)(
      implicit factory: SourceFactory
  ): Unit =
    if (componentLoggingState.doWarn || has(id, WARN)) all(WARN, id, msg, map, ex, factory.get())

  override def error(msg: ⇒ String, map: ⇒ Map[String, Any], ex: Throwable, id: AnyId)(
      implicit factory: SourceFactory
  ): Unit =
    if (componentLoggingState.doError || has(id, ERROR)) all(ERROR, id, msg, map, ex, factory.get())

  override def fatal(msg: ⇒ String, map: ⇒ Map[String, Any], ex: Throwable, id: AnyId)(
      implicit factory: SourceFactory
  ): Unit =
    all(FATAL, id, msg, map, ex, factory.get())

  private[logging] override def alternative(category: String,
                                            m: Map[String, RichMsg],
                                            ex: Throwable,
                                            id: AnyId,
                                            time: Long): Unit =
    MessageHandler.sendMsg(LogAltMessage(category, time, m ++ Map(LoggingKeys.CATEGORY -> category), id, ex))
}
