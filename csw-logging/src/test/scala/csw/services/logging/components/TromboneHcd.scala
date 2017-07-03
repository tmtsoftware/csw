package csw.services.logging.components

import csw.services.logging.scaladsl.{ComponentLogger, RichException}

object TromboneHcdLogger extends ComponentLogger("tromboneHcd")

class TromboneHcd() extends TromboneHcdLogger.Simple {

  // Do not add any lines before this method
  // Tests are written to assert on this line numbers
  // In case any line needs to be added then update constants in companion object
  def startLogging(logs: Map[String, String], kind: String = "all"): Unit = kind match {
    case "alternative" ⇒ log.alternative("some-alternative-category", Map("@msg" → logs("alternative")))
    case "all" ⇒ {
      log.trace(logs("trace"))
      log.debug(logs("debug"))
      log.info(logs("info"))
      log.warn(logs("warn"))
      log.error(logs("error"))
      log.fatal(logs("fatal"))
    }
  }

  def compute(number1: Int, number2: Int): String = {
    val exceptionMsg = "Exception occurred."

    try {
      val result = number1 / number2
      s"Result of computation is $result"
    } catch {
      case ex: ArithmeticException ⇒ log.error(exceptionMsg, ex); exceptionMsg
    }
  }

  def logRichException(message: String) =
    log.error(message, RichException("Rich Exception", new RuntimeException))

}

object TromboneHcd {
  val TRACE_LINE_NO = 15
  val DEBUG_LINE_NO = TRACE_LINE_NO + 1
  val INFO_LINE_NO  = TRACE_LINE_NO + 2
  val WARN_LINE_NO  = TRACE_LINE_NO + 3
  val ERROR_LINE_NO = TRACE_LINE_NO + 4
  val FATAL_LINE_NO = TRACE_LINE_NO + 5
}
