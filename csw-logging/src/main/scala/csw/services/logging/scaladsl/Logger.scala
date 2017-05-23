package csw.services.logging.scaladsl

import csw.services.logging.internal.LoggingLevels._
import csw.services.logging.internal.LoggingState._
import csw.services.logging.internal.{Log, LogAltMessage, MessageHandler}
import csw.services.logging.macros.{SourceLocation, SourceLocationFactory}
import csw.services.logging.{noException, RichMsg}

/**
 * This class provides the methods needed for logging.
 * It is accessed by including one of the traits ClassSupport or ActorSupport.
 */
class Logger private[logging] (componentName: Option[String], actorName: Option[String]) {

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

  /**
   * Writes a trace level log message.
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id optional id of a request
   */
  def trace(msg: => RichMsg, ex: Throwable = noException, id: AnyId = noId)(
      implicit factory: SourceLocationFactory
  ): Unit =
    if (doTrace || has(id, TRACE)) all(TRACE, id, msg, ex, factory.get())

  /**
   * Writes a debug level log message.
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id optional id of a request
   */
  def debug(msg: => RichMsg, ex: Throwable = noException, id: AnyId = noId)(
      implicit factory: SourceLocationFactory
  ): Unit =
    if (doDebug || has(id, DEBUG)) all(DEBUG, id, msg, ex, factory.get())

  /**
   * Writes an info level log message.
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id optional id of a request
   */
  def info(msg: => RichMsg, ex: Throwable = noException, id: AnyId = noId)(
      implicit factory: SourceLocationFactory
  ): Unit =
    if (doInfo || has(id, INFO)) all(INFO, id, msg, ex, factory.get())

  /**
   * Writes a warn level log message.
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id optional id of a request
   */
  def warn(msg: => RichMsg, ex: Throwable = noException, id: AnyId = noId)(
      implicit factory: SourceLocationFactory
  ): Unit =
    if (doWarn || has(id, WARN)) all(WARN, id, msg, ex, factory.get())

  /**
   * Writes an error level log message.
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id optional id of a request
   */
  def error(msg: => RichMsg, ex: Throwable = noException, id: AnyId = noId)(
      implicit factory: SourceLocationFactory
  ): Unit = if (doError || has(id, ERROR)) all(ERROR, id, msg, ex, factory.get())

  /**
   * Writes a fatal level log message.
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id optional id of a request
   */
  def fatal(msg: => RichMsg, ex: Throwable = noException, id: AnyId = noId)(
      implicit factory: SourceLocationFactory
  ): Unit = all(FATAL, id, msg, ex, factory.get())

  /**
   * Write a log message to an alternative log.
   * @param category the category for the message. For log files, this will be part of the file name. The following
   *                 categories are often used: server, client, gc, and time.
   * @param m  fields to be included in the log message.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id optional id of a request
   * @param time the time to be written in the log. If not specified the default is the time this
   *             method is called.
   */
  def alternative(category: String,
                  m: Map[String, RichMsg],
                  ex: Throwable = noException,
                  id: AnyId = noId,
                  time: Long = System.currentTimeMillis()): Unit =
    MessageHandler.sendMsg(LogAltMessage(category, time, m ++ Map("@category" -> category), id, ex))
}
