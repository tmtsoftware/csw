package csw.services.logging.components

import csw.services.logging.scaladsl.ComponentLogger

object SingletonTestLogger extends ComponentLogger("SingletonComponent")

object SingletonComponent extends SingletonTestLogger.Simple {

  // Do not add any lines before this method
  // Tests are written to assert on this line numbers
  // In case any line needs to be added then update ${LEVEL}_LINE_NO constants
  def startLogging(logs: Map[String, String]): Unit = {
    log.trace(logs("trace"))
    log.debug(logs("debug"))
    log.info(logs("info"))
    log.warn(logs("warn"))
    log.error(logs("error"))
    log.fatal(logs("fatal"))
  }

  val TRACE_LINE_NO = 13
  val DEBUG_LINE_NO = TRACE_LINE_NO + 1
  val INFO_LINE_NO  = TRACE_LINE_NO + 2
  val WARN_LINE_NO  = TRACE_LINE_NO + 3
  val ERROR_LINE_NO = TRACE_LINE_NO + 4
  val FATAL_LINE_NO = TRACE_LINE_NO + 5

  // A special startLogging for adding in a variable set of user keys and values
  // Do not add any lines before this method
  // Tests are written to assert on this line numbers
  // In case any line needs to be added then update ${LEVEL}_LINE_NO constants
  def startLogging(logs: Map[String, String], userMsgMap: Map[String, String]): Unit = {
    log.trace(logs("trace"), userMsgMap)
    log.debug(logs("debug"), userMsgMap)
    log.info(logs("info"), userMsgMap)
    log.warn(logs("warn"), userMsgMap)
    log.error(logs("error"), userMsgMap)
    log.fatal(logs("fatal"), userMsgMap)
  }

  val USER_TRACE_LINE_NO = 33
}
