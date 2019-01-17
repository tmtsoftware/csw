package csw.logging.client.exceptions

/**
 * As part of spawning the `LoggingSystem`, it looks for appenders (e.g. FileAppender, StdOutAppender, etc.) and tries to instantiate
 * it via reflection. In this process if any exception is encountered then AppenderNotFoundException is thrown.
 *
 * @param appender the name of appender that failed to instantiate
 */
case class AppenderNotFoundException(appender: String)
    extends RuntimeException(s"Could not find appender $appender specified in config file")
