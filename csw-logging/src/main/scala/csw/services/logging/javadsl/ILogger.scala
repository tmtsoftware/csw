package csw.services.logging.javadsl

import java.util.function.Supplier

import csw.services.logging.scaladsl.{AnyId, Logger}

trait ILogger {

  /**
   * Writes a trace level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def trace(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit
  def trace(msg: Supplier[Object], id: AnyId): Unit
  def trace(msg: Supplier[Object], ex: Throwable): Unit
  def trace(msg: Supplier[Object]): Unit

  /**
   * Writes a debug level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def debug(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit
  def debug(msg: Supplier[Object], id: AnyId): Unit
  def debug(msg: Supplier[Object], ex: Throwable): Unit
  def debug(msg: Supplier[Object]): Unit

  /**
   * Writes an info level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def info(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit
  def info(msg: Supplier[Object], id: AnyId): Unit
  def info(msg: Supplier[Object], ex: Throwable): Unit
  def info(msg: Supplier[Object]): Unit

  /**
   * Writes a warn level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def warn(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit
  def warn(msg: Supplier[Object], id: AnyId): Unit
  def warn(msg: Supplier[Object], ex: Throwable): Unit
  def warn(msg: Supplier[Object]): Unit

  /**
   * Writes an error level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def error(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit
  def error(msg: Supplier[Object], id: AnyId): Unit
  def error(msg: Supplier[Object], ex: Throwable): Unit
  def error(msg: Supplier[Object]): Unit

  /**
   * Writes a fatal level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def fatal(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit
  def fatal(msg: Supplier[Object], id: AnyId): Unit
  def fatal(msg: Supplier[Object], ex: Throwable): Unit
  def fatal(msg: Supplier[Object]): Unit

  /**
   * Write a log message to an alternative log.
   *
   * @param category the category for the message. For log files, this will be part of the file name. The following
   *                 categories are often used: server, client, gc, and time.
   * @param msg        fields to be included in the log message.
   * @param ex       an optional exception to be logged together with its stack trace.
   * @param id       optional id of a request
  **/
  def alternative(category: String, msg: java.util.Map[String, Object], ex: Throwable, id: AnyId): Unit
  def alternative(category: String, msg: java.util.Map[String, Object], id: AnyId): Unit
  def alternative(category: String, msg: java.util.Map[String, Object], ex: Throwable): Unit
  def alternative(category: String, msg: java.util.Map[String, Object]): Unit

  def asScala: Logger
}
