package csw.logging.client.exceptions

/**
 * As part of spawning the `LoggingSystem`, it looks for appenders (e.g. FileAppender, StdOutAppender, etc.) and tries to instantiate
 * it via reflection. In this process if any exception is encountered then AppenderNotFoundException is thrown.
 *
 * @param appender the name of appender that failed to instantiate
 */
case class AppenderNotFoundException(appender: String)
    extends RuntimeException(s"Could not find appender $appender specified in config file")

/**
 * This exception is thrown when TMT_LOG_HOME is not set as environment variable
 *
 * @param basePathHolder name of environment variable where log files would be stored i.e. TMT_LOG_PATH
 */
case class BaseLogPathNotDefined(basePathHolder: String)
    extends RuntimeException(
      s"Base log path is not defined as environment variable. " +
      s"Please set <$basePathHolder> to define base path to store log files from tmt applications."
    )
