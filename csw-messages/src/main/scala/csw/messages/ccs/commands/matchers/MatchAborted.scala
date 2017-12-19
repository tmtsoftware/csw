package csw.messages.ccs.commands.matchers

case class MatchAborted(prefix: String) extends RuntimeException(s"Matching demand state aborted for $prefix")
