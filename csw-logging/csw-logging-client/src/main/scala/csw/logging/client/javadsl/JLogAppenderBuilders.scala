package csw.logging.client.javadsl

import csw.logging.client.appenders
import csw.logging.client.appenders.LogAppenderBuilder

/**
 * Helper class for Java to get the handle of csw provided appenders
 */
object JLogAppenderBuilders {

  /**
   * Represents StdOut as appender for logs
   */
  val StdOutAppender: LogAppenderBuilder = appenders.StdOutAppender

  /**
   * Represents file as appender for logs
   */
  val FileAppender: LogAppenderBuilder = appenders.FileAppender
}
