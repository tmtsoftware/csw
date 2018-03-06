package csw.services.logging.appenders

import akka.actor.ActorRefFactory
import csw.services.logging.RichMsg

import scala.concurrent.Future

/**
 * Trait for log appender companion objects
 */
trait LogAppenderBuilder {

  /**
   * Log appender constructor
   *
   * @param factory         An Akka factory
   * @param standardHeaders The headers that are fixes for this service
   * @return                An appender
   */
  def apply(factory: ActorRefFactory, standardHeaders: Map[String, RichMsg]): LogAppender
}

/**
 * Trait for log appender classes.
 */
trait LogAppender {

  /**
   * Appends a new log message
   *
   * @param baseMsg   The message to be logged
   * @param category  The kinds of log (for example, "common")
   */
  def append(baseMsg: Map[String, RichMsg], category: String): Unit

  /**
   * Called just before the logger shuts down
   *
   * @return A future that is completed when finished
   */
  def finish(): Future[Unit]

  /**
   * Stops a log appender
   *
   * @return A future that is completed when stopped
   */
  def stop(): Future[Unit]

}
