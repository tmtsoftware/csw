package csw.messages.ccs.commands.matchers

/**
 * An exception depicting a match was aborted
 * @param prefix the subsystem identifier for which matching was in progress
 */
case class MatchAborted(prefix: String) extends RuntimeException(s"Matching demand state aborted for $prefix")
