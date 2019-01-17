package csw.logging.client.components

import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory

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
