package csw.services.logging.javadsl

import java.util.function.Supplier

import csw.services.logging.scaladsl.{AnyId, Logger}

trait ILogger {

  /**
   * Writes a trace level log message.
   *
   * @param msg the message to be logged
   */
  def trace(msg: Supplier[String]): Unit

  /**
   * Writes a trace level log message.
   *
   * @param msg the message to be logged
   * @param ex  exception to be logged together with its stack trace
   */
  def trace(msg: Supplier[String], ex: Throwable): Unit

  /**
   * Writes a trace level log message.
   *
   * @param msg the message to be logged
   * @param id  id of a request
   */
  def trace(msg: Supplier[String], id: AnyId): Unit

  /**
   * Writes a trace level log message.
   *
   * @param msg the message to be logged
   * @param ex  exception to be logged together with its stack trace
   * @param id  id of a request
   */
  def trace(msg: Supplier[String], ex: Throwable, id: AnyId): Unit

  /**
   * Writes a trace level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   */
  def trace(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]]): Unit

  /**
   * Writes a trace level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param ex  exception to be logged together with its stack trace
   */
  def trace(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable): Unit

  /**
   * Writes a trace level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param id  id of a request
   */
  def trace(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], id: AnyId): Unit

  /**
   * Writes a trace level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param ex  exception to be logged together with its stack trace
   * @param id  id of a request
   */
  def trace(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable, id: AnyId): Unit

  /**
   * Writes a debug level log message.
   *
   * @param msg the message to be logged
   */
  def debug(msg: Supplier[String]): Unit

  /**
   * Writes a debug level log message.
   *
   * @param msg the message to be logged
   * @param ex  exception to be logged together with its stack trace
   */
  def debug(msg: Supplier[String], ex: Throwable): Unit

  /**
   * Writes a debug level log message.
   *
   * @param msg the message to be logged
   * @param id  id of a request
   */
  def debug(msg: Supplier[String], id: AnyId): Unit

  /**
   * Writes a debug level log message.
   *
   * @param msg the message to be logged
   * @param ex  exception to be logged together with its stack trace
   * @param id  id of a request
   */
  def debug(msg: Supplier[String], ex: Throwable, id: AnyId): Unit

  /**
   * Writes a debug level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   */
  def debug(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]]): Unit

  /**
   * Writes a debug level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param ex  exception to be logged together with its stack trace
   */
  def debug(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable): Unit

  /**
   * Writes a debug level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param id  id of a request
   */
  def debug(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], id: AnyId): Unit

  /**
   * Writes a debug level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param ex  exception to be logged together with its stack trace
   * @param id  id of a request
   */
  def debug(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable, id: AnyId): Unit

  /**
   * Writes an info level log message.
   *
   * @param msg the message to be logged
   */
  def info(msg: Supplier[String]): Unit

  /**
   * Writes an info level log message.
   *
   * @param msg the message to be logged
   * @param ex  an exception to be logged together with its stack trace
   */
  def info(msg: Supplier[String], ex: Throwable): Unit

  /**
   * Writes an info level log message.
   *
   * @param msg the message to be logged
   * @param id  id of a request
   */
  def info(msg: Supplier[String], id: AnyId): Unit

  /**
   * Writes a info level log message.
   *
   * @param msg the message to be logged
   * @param ex  exception to be logged together with its stack trace
   * @param id  id of a request
   */
  def info(msg: Supplier[String], ex: Throwable, id: AnyId): Unit

  /**
   * Writes an info level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   */
  def info(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]]): Unit

  /**
   * Writes an info level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param ex  an exception to be logged together with its stack trace
   */
  def info(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable): Unit

  /**
   * Writes an info level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param id  id of a request
   */
  def info(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], id: AnyId): Unit

  /**
   * Writes an info level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param ex  an exception to be logged together with its stack trace
   * @param id  id of a request
   */
  def info(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable, id: AnyId): Unit

  /**
   * Writes a warn level log message.
   *
   * @param msg the message to be logged
   */
  def warn(msg: Supplier[String]): Unit

  /**
   * Writes a warn level log message.
   *
   * @param msg the message to be logged
   * @param ex  an exception to be logged together with its stack trace
   */
  def warn(msg: Supplier[String], ex: Throwable): Unit

  /**
   * Writes a warn level log message.
   *
   * @param msg the message to be logged
   * @param id  id of a request
   */
  def warn(msg: Supplier[String], id: AnyId): Unit

  /**
   * Writes a warn level log message.
   *
   * @param msg the message to be logged
   * @param ex  exception to be logged together with its stack trace
   * @param id  id of a request
   */
  def warn(msg: Supplier[String], ex: Throwable, id: AnyId): Unit

  /**
   * Writes a warn level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   */
  def warn(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]]): Unit

  /**
   * Writes a warn level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param ex  an exception to be logged together with its stack trace
   */
  def warn(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable): Unit

  /**
   * Writes a warn level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param id  id of a request
   */
  def warn(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], id: AnyId): Unit

  /**
   * Writes a warn level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param ex  an exception to be logged together with its stack trace
   * @param id  id of a request
   */
  def warn(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable, id: AnyId): Unit

  /**
   * Writes an error level log message.
   *
   * @param msg the message to be logged
   */
  def error(msg: Supplier[String]): Unit

  /**
   * Writes an error level log message.
   *
   * @param msg the message to be logged
   * @param ex  an exception to be logged together with its stack trace
   */
  def error(msg: Supplier[String], ex: Throwable): Unit

  /**
   * Writes an error level log message.
   *
   * @param msg the message to be logged
   * @param id  id of a request
   */
  def error(msg: Supplier[String], id: AnyId): Unit

  /**
   * Writes a error level log message.
   *
   * @param msg the message to be logged
   * @param ex  exception to be logged together with its stack trace
   * @param id  id of a request
   */
  def error(msg: Supplier[String], ex: Throwable, id: AnyId): Unit

  /**
   * Writes an error level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   */
  def error(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]]): Unit

  /**
   * Writes an error level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param ex  an exception to be logged together with its stack trace
   */
  def error(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable): Unit

  /**
   * Writes an error level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param id  id of a request
   */
  def error(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], id: AnyId): Unit

  /**
   * Writes an error level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param ex  an exception to be logged together with its stack trace
   * @param id  id of a request
   */
  def error(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable, id: AnyId): Unit

  /**
   * Writes a fatal level log message.
   *
   * @param msg the message to be logged
   */
  def fatal(msg: Supplier[String]): Unit

  /**
   * Writes a fatal level log message.
   *
   * @param msg the message to be logged
   * @param ex  an exception to be logged together with its stack trace
   */
  def fatal(msg: Supplier[String], ex: Throwable): Unit

  /**
   * Writes a fatal level log message.
   *
   * @param msg the message to be logged
   * @param id  id of a request
   */
  def fatal(msg: Supplier[String], id: AnyId): Unit

  /**
   * Writes a fatal level log message.
   *
   * @param msg the message to be logged
   * @param ex  exception to be logged together with its stack trace
   * @param id  id of a request
   */
  def fatal(msg: Supplier[String], ex: Throwable, id: AnyId): Unit

  /**
   * Writes a fatal level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   */
  def fatal(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]]): Unit

  /**
   * Writes a fatal level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param ex  an exception to be logged together with its stack trace
   */
  def fatal(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable): Unit

  /**
   * Writes a fatal level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param id  id of a request
   */
  def fatal(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], id: AnyId): Unit

  /**
   * Writes a fatal level log message.
   *
   * @param msg the message to be logged
   * @param map key-value pairs to be logged along with message
   * @param ex  an exception to be logged together with its stack trace
   * @param id  id of a request
   */
  def fatal(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable, id: AnyId): Unit

  def asScala: Logger
}
