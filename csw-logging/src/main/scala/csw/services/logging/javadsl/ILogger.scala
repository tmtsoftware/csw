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

  /**
   * Writes a trace level log message.
   *
   * @param msg the message to be logged.
   * @param id  optional id of a request
   */
  def trace(msg: Supplier[Object], id: AnyId): Unit

  /**
   * Writes a trace level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   */
  def trace(msg: Supplier[Object], ex: Throwable): Unit

  /**
   * Writes a trace level log message.
   *
   * @param msg the message to be logged.
   */
  def trace(msg: Supplier[Object]): Unit

  /**
   * Writes a debug level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def debug(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit

  /**
   * Writes a debug level log message.
   *
   * @param msg the message to be logged.
   * @param id  optional id of a request
   */
  def debug(msg: Supplier[Object], id: AnyId): Unit

  /**
   * Writes a debug level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   */
  def debug(msg: Supplier[Object], ex: Throwable): Unit

  /**
   * Writes a debug level log message.
   *
   * @param msg the message to be logged.
   */
  def debug(msg: Supplier[Object]): Unit

  /**
   * Writes an info level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def info(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit

  /**
   * Writes an info level log message.
   *
   * @param msg the message to be logged.
   * @param id  optional id of a request
   */
  def info(msg: Supplier[Object], id: AnyId): Unit

  /**
   * Writes an info level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   */
  def info(msg: Supplier[Object], ex: Throwable): Unit

  /**
   * Writes an info level log message.
   *
   * @param msg the message to be logged.
   */
  def info(msg: Supplier[Object]): Unit

  /**
   * Writes a warn level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def warn(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit

  /**
   * Writes a warn level log message.
   *
   * @param msg the message to be logged.
   * @param id  optional id of a request
   */
  def warn(msg: Supplier[Object], id: AnyId): Unit

  /**
   * Writes a warn level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   */
  def warn(msg: Supplier[Object], ex: Throwable): Unit

  /**
   * Writes a warn level log message.
   *
   * @param msg the message to be logged.
   */
  def warn(msg: Supplier[Object]): Unit

  /**
   * Writes an error level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def error(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit

  /**
   * Writes an error level log message.
   *
   * @param msg the message to be logged.
   * @param id  optional id of a request
   */
  def error(msg: Supplier[Object], id: AnyId): Unit

  /**
   * Writes an error level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   */
  def error(msg: Supplier[Object], ex: Throwable): Unit

  /**
   * Writes an error level log message.
   *
   * @param msg the message to be logged.
   */
  def error(msg: Supplier[Object]): Unit

  /**
   * Writes a fatal level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def fatal(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit

  /**
   * Writes a fatal level log message.
   *
   * @param msg the message to be logged.
   * @param id  optional id of a request
   */
  def fatal(msg: Supplier[Object], id: AnyId): Unit

  /**
   * Writes a fatal level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   */
  def fatal(msg: Supplier[Object], ex: Throwable): Unit

  /**
   * Writes a fatal level log message.
   *
   * @param msg the message to be logged.
   */
  def fatal(msg: Supplier[Object]): Unit

  def asScala: Logger
}
