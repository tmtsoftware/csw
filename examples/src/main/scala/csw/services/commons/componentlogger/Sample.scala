package csw.services.commons.componentlogger

import csw.services.logging.scaladsl.{Logger, LoggerFactory}

//#component-logger
class Sample(_componentName: String) {
  val log: Logger = new LoggerFactory(_componentName).getLogger
}
//#component-logger
