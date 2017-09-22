package csw.services.logging.exceptions

case class AppenderNotFoundException(appender: String)
    extends RuntimeException(s"Could not find appender $appender specified in config file")
