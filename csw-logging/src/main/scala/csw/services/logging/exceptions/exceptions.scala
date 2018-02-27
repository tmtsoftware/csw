package csw.services.logging.exceptions

//TODO: explain better significance when is it used
case class AppenderNotFoundException(appender: String)
    extends RuntimeException(s"Could not find appender $appender specified in config file")
