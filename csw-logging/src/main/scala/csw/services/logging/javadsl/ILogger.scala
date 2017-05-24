package csw.services.logging.javadsl

import java.util.function.Supplier

import csw.services.logging.RichMsg
import csw.services.logging.scaladsl.AnyId

trait ILogger {

  /**
   * Writes a trace level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def info(msg: Supplier[RichMsg], ex: Throwable, id: AnyId): Unit
  def info(msg: Supplier[RichMsg], id: AnyId): Unit
  def info(msg: Supplier[RichMsg], ex: Throwable): Unit
  def info(msg: Supplier[RichMsg]): Unit
}
