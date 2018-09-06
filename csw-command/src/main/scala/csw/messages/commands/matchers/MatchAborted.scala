package csw.messages.commands.matchers

/**
 * An exception depicting a match was aborted
 *
 * @param prefix the subsystem identifier for which matching was in progress
 */
case class MatchAborted private[messages] (prefix: String) extends RuntimeException(s"Matching demand state aborted for $prefix")
