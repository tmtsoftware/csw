package csw.logging.components

import csw.logging.scaladsl.{Logger, LoggerFactory}

class TromboneAssembly(loggerFactory: LoggerFactory) {

  val log: Logger = loggerFactory.getLogger

  def startLogging(logs: Map[String, String]): Unit = {
    log.trace(logs("trace"))
    log.debug(logs("debug"))
    log.info(logs("info"))
    log.warn(logs("warn"))
    log.error(logs("error"))
    log.fatal(logs("fatal"))
  }
}
