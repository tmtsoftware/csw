package csw.services.ccs.exceptions

case class MatchAborted(prefix: String) extends RuntimeException(s"Matching demand state aborted for $prefix")
