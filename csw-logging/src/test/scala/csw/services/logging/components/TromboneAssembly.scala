package csw.services.logging.components

import csw.services.logging.scaladsl.ComponentLogger

class TromboneAssembly(compName: String) extends ComponentLogger.Simple {
  def startLogging(logs: Map[String, String]): Unit = {
    log.trace(logs("trace"))
    log.debug(logs("debug"))
    log.info(logs("info"))
    log.warn(logs("warn"))
    log.error(logs("error"))
    log.fatal(logs("fatal"))
  }

  override protected def componentName(): String = compName
}
